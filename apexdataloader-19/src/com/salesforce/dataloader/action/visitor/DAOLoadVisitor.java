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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.log4j.Logger;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataReader;
import com.salesforce.dataloader.dao.DataWriter;
import com.salesforce.dataloader.dyna.SforceDynaBean;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.LoadException;
import com.salesforce.dataloader.mapping.MappingManager;
import com.salesforce.dataloader.util.LoadRateCalculator;
import com.sforce.async.AsyncApiException;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.ws.ConnectionException;

/**
 * Visitor to convert rows into Dynamic objects
 *
 * @author lviripaeff
 * @since before
 */
public abstract class DAOLoadVisitor implements DAORowVisitor {

    protected Controller controller;
    protected List<String> columnNames;

    // this stores the dynabeans, which convert types correctly
    protected List<DynaBean> dynaArray;
    protected List<Map<String, Object>> dataArray;

    protected MappingManager mapper;
    protected BasicDynaClass dynaClass;
    protected DynaProperty[] dynaProps;

    // logger
    public static Logger logger = Logger.getLogger(DAOLoadVisitor.class);
    //protected ILoaderProgress monitor;
    protected DataWriter successWriter;
    protected DataWriter errorWriter;
    protected LoadRateCalculator subTaskCalc;
    protected int batchSize;
    protected Config config;
    protected int numSuccess = 0;
    protected int numErrors = 0;

    public DAOLoadVisitor(Controller controller, DataWriter successWriter,
            DataWriter errorWriter, LoadRateCalculator subTaskCalc) {
        this.controller = controller;
      //  this.monitor = monitor;
        this.successWriter = successWriter;
        this.errorWriter = errorWriter;
        this.subTaskCalc = subTaskCalc;
        this.config = controller.getConfig();
        this.batchSize = config.getLoadBatchSize();

        DataReader dataReader = (DataReader)controller.getDao();
        columnNames = dataReader.getColumnNames();

        dynaArray = new LinkedList<DynaBean>();
        dataArray = new LinkedList<Map<String, Object>>();

        mapper = controller.getMappingManager();

        SforceDynaBean.registerConverters(config.getBoolean(Config.EURO_DATES));

        dynaProps = SforceDynaBean.createDynaProps(controller.getFieldTypes(), controller);
        dynaClass = SforceDynaBean.getDynaBeanInstance(dynaProps); 
    }

    public void visit(Map<String, Object> row) throws LoadException, DataAccessObjectException, ConnectionException {
        // the result are sforce fields mapped to data
        HashMap<String, Object> sforceDataRow = mapper.mapData(row);
        hook_preConvert(sforceDataRow);
        try {
            dynaArray.add(SforceDynaBean.convertToDynaBean(dynaClass, sforceDataRow));
        } catch (ConversionException conve) {
            logger.error(Messages.getString("Visitor.conversionException"), conve); //$NON-NLS-1$
            Map<String, Object> errorRow = new HashMap<String, Object>(row);
            errorRow.put(Config.ERROR_COLUMN_NAME, Messages.getFormattedString(
                    "Visitor.conversionErrorMsg", conve.getMessage())); //$NON-NLS-1$
            errorWriter.writeRow(errorRow);
            numErrors++;
            // this row cannot be added since conversion has failed
            return;
        }

        // add the data for writing to the result files
        // must do this after conversion.
        dataArray.add(row);
        // load the batch
        if (dynaArray.size() >= batchSize) {
            loadBatch();
        }

    }

    protected void hook_preConvert(Map<String, Object> row) {}

    public void flushRemaining() throws LoadException, DataAccessObjectException {
        // check if there are any entities left
        if (dynaArray.size() > 0) {
            loadBatch();
        }
    }

    protected abstract void loadBatch() throws DataAccessObjectException, LoadException;

    public int getNumSuccess() {
        return numSuccess;
    }

    public int getNumErrors() {
        return numErrors;
    }

    public void clearArrays() {
        // clear the arrays
        dataArray.clear();
        dynaArray.clear();
    }

    protected void handleException(String msgOverride, Throwable t) throws LoadException {
        String msg = msgOverride;
        if (msg == null) {
            msg = t.getMessage();
            if (t instanceof AsyncApiException) {
                msg = ((AsyncApiException)t).getExceptionMessage();
            } else if (t instanceof ApiFault) {
                msg = ((ApiFault)t).getExceptionMessage();
            }
        }
        throw new LoadException(msg, t);
    }

    protected void handleException(Throwable t) throws LoadException {
        handleException(null, t);
    }
}
