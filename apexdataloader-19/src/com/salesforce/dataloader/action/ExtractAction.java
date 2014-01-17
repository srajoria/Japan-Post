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

package com.salesforce.dataloader.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.action.visitor.QueryVisitor;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataAccessObjectFactory;
import com.salesforce.dataloader.dao.DataWriter;
import com.salesforce.dataloader.dao.csv.CSVFileWriter;
import com.salesforce.dataloader.exception.*;
import com.salesforce.dataloader.mapping.MappingManager;
import com.sforce.soap.partner.Field;

/**
 * @author lviripaeff
 * @since before
 */
public class ExtractAction implements IAction {
    protected ILoaderProgress monitor = null;
    protected Controller controller;
    protected Config config;
    protected DataWriter extractWriter;
    protected DataWriter errorWriter;
    protected DataWriter successWriter;
    protected QueryVisitor visitor;
    protected String soql;

    //logger
    private static Logger logger = Logger.getLogger(ExtractAction.class);

    public ExtractAction(Controller controller, ILoaderProgress monitor) throws DataAccessObjectInitializationException {
        //instantiate our class variables
        this.monitor = monitor;
        this.controller = controller;
        config = this.controller.getConfig();
        // validate that the right data access object is passed in before casting
        if (! (this.controller.getDao() instanceof DataWriter)) {
            String errMsg = Messages.getFormattedString("Action.errorWrongDao", new String[] {
                    config.getString(Config.DAO_TYPE),
                    DataAccessObjectFactory.CSV_WRITE_TYPE + " or " + DataAccessObjectFactory.DATABASE_WRITE_TYPE,
                    config.getString(Config.OPERATION) });
            logger.fatal(errMsg);
            throw new DataAccessObjectInitializationException(errMsg);
        }
        extractWriter = (DataWriter)this.controller.getDao();
        if(config.getBoolean(Config.ENABLE_EXTRACT_STATUS_OUTPUT)) {
            successWriter = getSuccessWriter(config.getBoolean(Config.WRITE_UTF8));
            errorWriter = getErrorWriter(config.getBoolean(Config.WRITE_UTF8));
        }
        soql = config.getString(Config.EXTRACT_SOQL);
    }

    /**
     * This is the main method. Call this method to execute the action
     */
    public void execute() {

        try {
            // get columns that will be output from the query and open the outputs
            List<String> extractColumns = getColumnsFromSoql(soql);
            List<String> daoColumns = getDaoColumns(extractColumns);

            extractWriter.open();
            extractWriter.setColumnNames(daoColumns);

            if(config.getBoolean(Config.ENABLE_EXTRACT_STATUS_OUTPUT)) {
                openSuccessWriter(daoColumns);
                openErrorWriter(daoColumns);
            }

            visitor = new QueryVisitor(controller, monitor, extractWriter, successWriter, errorWriter, extractColumns);
            visitor.visit(soql);

            boolean wasCanceled = false;

            //loop through and query more
            while (visitor.hasMore()) {
                //check if canceled
                //check if the monitor was cancelled
                if (monitor.isCanceled()) {
                    wasCanceled = true;
                    break;
                }
                //query more on the locator
                visitor.visitMore(visitor.getQueryLocator());
            }

            //need to close up here for sync message.
            closeWriters();

            String[] args = { String.valueOf(visitor.getNumSuccess()), config.getString(Config.OPERATION),
                    String.valueOf(visitor.getNumErrors()) };

            //set the monitor to done
            if (wasCanceled) {
                monitor.doneSuccess(Messages.getFormattedString("ExtractAction.cancel", args)); //$NON-NLS-1$
            } else {
                monitor.doneSuccess(Messages.getFormattedString("ExtractAction.success", args)); //$NON-NLS-1$
            }

        } catch (ExtractException ex) {
            monitor.doneError(ex.getMessage());
        } catch (DataAccessObjectException ex) {
            monitor.doneError(ex.getMessage());
        } finally {
            closeWriters();
        }

    }

    /**
     *
     */
    private void closeWriters() {
        extractWriter.close();
        if(config.getBoolean(Config.ENABLE_EXTRACT_STATUS_OUTPUT)) {
            successWriter.close();
            errorWriter.close();
        }
    }

    /**
     * @param extractColumns
     * @return List of data access object columns according to the field map
     */
    private List<String> getDaoColumns(List<String> extractColumns) {
        // Detect whether the map has been provided, if not, then initialize default mappings based on SOQL columns.
        // Extraction is special in that the map is not always required. also, the columns are
        // not known in advance since the extraction SOQL is parsed later on
        MappingManager mapper = controller.getMappingManager();
        if(!mapper.isMappingExists()) {
            Field[] fields = controller.getFieldTypes().getFields();
            List<String> sfdcFieldList = new ArrayList<String>();
            for(Field field : fields) {
                sfdcFieldList.add(field.getName());
            }
            mapper.initializeMapping(sfdcFieldList, extractColumns, true);
        }
        List<String> daoColumns = controller.getMappingManager().mapColumns(extractColumns);
        return daoColumns;
    }

    /**
     * @return Success Writer
     * @throws DataAccessObjectInitializationException
     */
    private DataWriter getSuccessWriter(boolean writeUtf8) throws DataAccessObjectInitializationException  {
        String filename = config.getString(Config.OUTPUT_SUCCESS);
        if(filename == null || filename.length() == 0) {
            throw new DataAccessObjectInitializationException(Messages.getString("ExtractAction.errorMissingSuccessFile"));
        }
        //TODO: Make sure that specific DAO is not mentioned: use DataReader, DataWriter, or DataAccessObject
        return (new CSVFileWriter(filename, writeUtf8, true));
    }

    /**
     * @return Error Writer
     * @throws DataAccessObjectInitializationException
     */
    private DataWriter getErrorWriter(boolean writeUtf8) throws DataAccessObjectInitializationException {
        String filename = config.getString(Config.OUTPUT_ERROR);
        if(filename == null || filename.length() == 0) {
            throw new DataAccessObjectInitializationException(Messages.getString("ExtractAction.errorMissingErrorFile"));
        }
        //TODO: Make sure that specific DAO is not mentioned: use DataReader, DataWriter, or DataAccessObject
        return (new CSVFileWriter(filename, writeUtf8, true));
    }

    private void openSuccessWriter(List<String> header) throws ExtractException {
        List<String> headerSuccess = new LinkedList<String>(header);
        //add the ID column if not there already
        if(!Config.ID_COLUMN_NAME.equals(headerSuccess.get(0))) {
            headerSuccess.add(0, Config.ID_COLUMN_NAME);
        }
        //add the STATUS column for data status detail
        headerSuccess.add(Config.STATUS_COLUMN_NAME);
        try {
            successWriter.open();
            successWriter.setColumnNames(headerSuccess);
        } catch (DataAccessObjectInitializationException e) {
            throw new ExtractException(Messages.getFormattedString("ExtractAction.errorOpeningSuccessFile", config.getString(Config.OUTPUT_SUCCESS)), e);
        }
    }

    private void openErrorWriter(List<String> header) throws ExtractException {
        List<String> headerError = new LinkedList<String>(header);
        //add the ERROR column for error message detail
        headerError.add(Config.ERROR_COLUMN_NAME);
        try {
            errorWriter.open();
            errorWriter.setColumnNames(headerError);
        } catch (DataAccessObjectInitializationException e) {
            throw new ExtractException(Messages.getFormattedString("ExtractAction.errorOpeningErrorFile", config.getString(Config.OUTPUT_ERROR)), e);
        }
    }

    public static List<String> getColumnsFromSoql(String soql) throws ExtractException {

        if(soql == null || soql.length() == 0) {
            String errMsg = Messages.getString("ExtractAction.errorEmptyQuery"); //$NON-NLS-1$
            logger.error(errMsg);
            throw new ExtractException(errMsg);
        }

        // normalize the SOQL string and find the field list
        String trimmedSoql = soql.trim().replaceAll("[\\s]*,[\\s]*",",");
        String upperSOQL = trimmedSoql.toUpperCase();
        int selectPos = upperSOQL.indexOf("SELECT ");
        if (selectPos == -1) {
            String errMsg = Messages.getFormattedString("ExtractAction.errorMissingSelect", soql);
            logger.error(errMsg);
            throw new ExtractException(errMsg);
        }
        int fieldListStart = selectPos + "SELECT ".length(); //$NON-NLS-1$
        int fieldListEnd = upperSOQL.indexOf(" FROM "); //$NON-NLS-1$

        try {
            String fieldString = trimmedSoql.substring(fieldListStart, fieldListEnd).trim();
            String[] fields = fieldString.split(","); //$NON-NLS-1$
            return new ArrayList<String>(Arrays.asList(fields));
        } catch (Exception e) {
            String errMsg;
            if (fieldListStart < "SELECT ".length()) {
                errMsg = Messages.getFormattedString("ExtractAction.errorMissingSelect", soql); //$NON-NLS-1$
            } else if (fieldListEnd < 0) {
                errMsg = Messages.getFormattedString("ExtractAction.errorMissingFrom", soql); //$NON-NLS-1$
            } else {
                errMsg = Messages.getFormattedString("ExtractAction.errorMalformedQuery", soql); //$NON-NLS-1$
            }
            logger.error(errMsg, e);
            throw new ExtractException(errMsg);
        }
    }
}

