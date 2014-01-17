/*
 * Copyright (c) 2005, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.dataloader.action.visitor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataWriter;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;
import com.salesforce.dataloader.exception.ExtractException;
import com.salesforce.dataloader.exception.ParameterLoadException;
import com.salesforce.dataloader.mapping.MappingManager;
import com.salesforce.dataloader.util.LoadRateCalculator;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.XmlObject;

/**
 * Visitor to convert rows into Dynamic objects
 * 
 * @author lviripaeff
 * @author awarshavsky
 * @since before
 */
public class QueryVisitor {

    protected Controller controller;
    // logger
    private static Logger logger = Logger.getLogger(QueryVisitor.class);
    protected ILoaderProgress monitor;
    protected DataWriter queryWriter;
    protected DataWriter errorWriter;
    protected DataWriter successWriter;
    protected LoadRateCalculator subTaskCalc;
    protected Config config;
    protected QueryResult qr;
    protected int recordNumber = 0;
    protected int numErrors = 0;
    protected int numSuccess = 0;
    protected List extractionColumns;
    protected MappingManager mapper;

    public QueryVisitor(Controller controller, ILoaderProgress monitor, DataWriter queryWriter,
            DataWriter successWriter, DataWriter errorWriter, List<String> extractionColumns) {
        this.controller = controller;
        this.mapper = controller.getMappingManager();
        this.monitor = monitor;
        this.queryWriter = queryWriter;
        this.errorWriter = errorWriter;
        this.successWriter = successWriter;
        this.config = controller.getConfig();
        this.extractionColumns = extractionColumns;
    }

    public void visit(String soql) throws ExtractException, DataAccessObjectException {
        try {
            // run the initial query
            qr = controller.getPartnerClient().query(soql);

            if (qr.getRecords() == null) {
                logger.info(Messages.getString("QueryVisitor.noneReturned")); //$NON-NLS-1$
                return;
            }

            // start the Progress Monitor
            monitor.beginTask(Messages.getString("QueryVisitor.extracting"), qr.getSize()); //$NON-NLS-1$

            subTaskCalc = new LoadRateCalculator(qr.getSize());
            subTaskCalc.start();

            int currentLength = qr.getRecords().length;
            recordNumber += currentLength;

            // write the record out
            writeExtraction(qr);

            // report progress
            monitor.worked(currentLength);
            monitor.setSubTask(subTaskCalc.calculateSubTask(currentLength, recordNumber, getNumSuccess(),
                    getNumErrors()));
        } catch (ApiFault e) {
            throw new ExtractException(e.getExceptionMessage(), e);
        } catch (ConnectionException e) {
            throw new ExtractException(e.getMessage(), e);
        }
    }

    public void visitMore(String ql) throws ExtractException, DataAccessObjectException {
        try {
            qr = controller.getPartnerClient().queryMore(ql);
            writeExtraction(qr);

            int currentLength = qr.getRecords().length;
            recordNumber += currentLength;
            monitor.worked(currentLength);
            monitor.setSubTask(subTaskCalc.calculateSubTask(currentLength, recordNumber, getNumSuccess(),
                    getNumErrors()));
        } catch (ConnectionException e) {
            throw new ExtractException(e);
        }

    }

    // decode '.' notation for related child objects in results
    private void extractResults(String prefix, Map<String, Object> out, XmlObject in) {
        Iterator<XmlObject> iter = in.getChildren();
        if (iter == null) return;

        while (iter.hasNext()) {
            XmlObject field = iter.next();
            String label = prefix == null ? field.getName().getLocalPart() : prefix + "."
                    + field.getName().getLocalPart();
            out.put(label, field.getValue());

            // Recursively iterate through the children.
            extractResults(label, out, field);
        }
    }

    private void writeExtraction(QueryResult qr) throws DataAccessObjectException {
        // form a map, because we aren't guaranteed to get back all the fields
        SObject sob;
        SObject[] sfdcResults = qr.getRecords();
        if (sfdcResults == null) {
            logger.error(Messages.getString("QueryVisitor.errorNoResults")); //$NON-NLS-1$
            return;
        }
        int daoBatchSize;
        try {
            daoBatchSize = config.getInt(Config.DAO_WRITE_BATCH_SIZE);
            if (daoBatchSize > Config.MAX_DAO_WRITE_BATCH_SIZE) {
                daoBatchSize = Config.MAX_DAO_WRITE_BATCH_SIZE;
            }
        } catch (ParameterLoadException e) {
            // warn about getting batch size parameter, otherwise continue w/ default
            logger.warn(Messages.getFormattedString("QueryVisitor.errorGettingBatchSize", new String[] {
                    String.valueOf(Config.DEFAULT_DAO_WRITE_BATCH_SIZE), e.getMessage() }));
            daoBatchSize = Config.DEFAULT_DAO_WRITE_BATCH_SIZE;
        }
        List<Map<String, Object>> daoRowList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < sfdcResults.length; i++) {
            HashMap<String, Object> sfdcRow;
            HashMap<String, Object> daoRow;

            sob = sfdcResults[i];
            Iterator<XmlObject> fieldIter = sob.getChildren();
            sfdcRow = new HashMap<String, Object>();
            
            // unfold the relationships fields 
            extractResults(null, sfdcRow, sob);

            while (fieldIter.hasNext()) {
                final XmlObject field = fieldIter.next();
                sfdcRow.put(field.getName().getLocalPart(), convertFieldValue(field.getValue()));
            }

            // add Id if query request did not contain it
            if (!sfdcRow.containsKey("Id")) {
                sfdcRow.put("Id", sob.getId()); //$NON-NLS-1$
            }

            // Map values from salesforce field names to the DAO column names
            daoRow = mapper.mapData(sfdcRow);

            // add row to batch
            daoRowList.add(daoRow);
            try {
                if (daoRowList.size() == daoBatchSize || (i + 1) == sfdcResults.length) {
                    boolean success = queryWriter.writeRowList(daoRowList);
                    if (success) {
                        numSuccess += daoRowList.size();
                    } else {
                        numErrors += daoRowList.size();
                    }

                    // save successful output status if requested
                    if (config.getBoolean(Config.ENABLE_EXTRACT_STATUS_OUTPUT)) {
                        // FIXME: All results will be the same, don't process every result. Does it make sense to add
                        // specific result processing in the future?

                        // add statuses and id's
                        DataWriter statusWriter;
                        if (success) {
                            statusWriter = successWriter;
                        } else {
                            statusWriter = errorWriter;
                        }
                        for (Map<String, Object> daoRowInput : daoRowList) {

                            // add Id if query request did not contain it
                            if (!daoRowInput.containsKey(Config.ID_COLUMN_NAME)) {
                                daoRowInput.put(Config.ID_COLUMN_NAME, sob.getId()); //$NON-NLS-1$
                            }

                            if (success) {
                                daoRowInput.put(Config.STATUS_COLUMN_NAME, Messages
                                        .getString("QueryVisitor.statusItemQueried"));
                            } else {
                                daoRowInput.put(Config.ERROR_COLUMN_NAME, Messages.getFormattedString(
                                        "QueryVisitor.statusErrorNotWritten", config.getString(Config.DAO_NAME))); //$NON-NLS-1$
                            }
                        }
                        statusWriter.writeRowList(daoRowList);
                    }
                    // start new batch of rows
                    daoRowList = new ArrayList<Map<String, Object>>();
                }
            } catch (DataAccessObjectInitializationException ex) {
                // initialization error is more fatal. the rest of writing does not make sense at this point
                throw ex;
            } catch (DataAccessObjectException ex) {
                numErrors += daoRowList.size();
                // save successful output status if requested
                if (config.getBoolean(Config.ENABLE_EXTRACT_STATUS_OUTPUT)) {
                    for (Map<String, Object> daoErrorRow : daoRowList) {
                        daoErrorRow.put(Config.ERROR_COLUMN_NAME, Messages.getFormattedString(
                                "QueryVisitor.statusErrorNotWrittenException", new String[] {
                                        config.getString(Config.DAO_NAME), ex.getMessage() }));
                    }
                    try {
                        errorWriter.writeRowList(daoRowList);
                    } catch (DataAccessObjectException daoe) {
                        // if error writing failed, throw an exception
                        throw ex;
                    }
                }
                // start new batch of rows
                daoRowList = new ArrayList<Map<String, Object>>();
            }
        }

	}

	private Object convertFieldValue(Object fieldVal) {
		if (fieldVal instanceof Calendar) {
			final DateFormat df = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			df.setCalendar((Calendar) fieldVal);
			return df.format(((Calendar) fieldVal).getTime());
		}

		if (fieldVal instanceof Date) {
			final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			return df.format((Date) fieldVal);
		}

		return fieldVal;
	}

	public String getQueryLocator() {
        return qr.getQueryLocator();
    }

    public boolean hasMore() {
        if (qr == null) { return false; }
        return !qr.getDone();
    }

    public int getNumErrors() {
        return numErrors;
    }

    public void setNumErrors(int numErrors) {
        this.numErrors = numErrors;
    }

    public int getNumSuccess() {
        return numSuccess;
    }

    public void setNumSuccess(int numSuccess) {
        this.numSuccess = numSuccess;
    }
}
