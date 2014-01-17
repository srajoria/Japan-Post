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

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.action.visitor.DAOLoadVisitor;
import com.salesforce.dataloader.config.*;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.*;
import com.salesforce.dataloader.dao.csv.CSVFileWriter;
import com.salesforce.dataloader.exception.*;
import com.salesforce.dataloader.util.DAORowUtil;
import com.salesforce.dataloader.util.LoadRateCalculator;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.ws.ConnectionException;

/**
 * @author lviripaeff
 * @since before
 */
public abstract class AbstractLoadAction implements IAction {
   //protected ILoaderProgress monitor = null;
    protected Controller controller;
    protected Config config;
    protected DataReader dataReader;
    protected DAOLoadVisitor visitor;
    protected LoadRateCalculator subTaskCalc;
    protected DataWriter successWriter;
    protected DataWriter errorWriter;

    // logger
    private static Logger logger = Logger.getLogger(AbstractLoadAction.class);
    private boolean wasCancelled;

    public AbstractLoadAction(Controller controller)
            throws DataAccessObjectInitializationException {
        // instantiate our class variables
       // this.monitor = monitor;
        this.controller = controller;
        config = this.controller.getConfig();

        // validate that the right data access object is passed in before casting
        if (!(this.controller.getDao() instanceof DataReader)) {
            String errMsg = Messages.getFormattedString("Action.errorWrongDao", new String[] {
                    config.getString(Config.DAO_TYPE),
                    DataAccessObjectFactory.CSV_READ_TYPE + " or " + DataAccessObjectFactory.DATABASE_READ_TYPE,
                    config.getString(Config.OPERATION) });
            logger.fatal(errMsg);
            throw new DataAccessObjectInitializationException(errMsg);
        }
        dataReader = (DataReader)this.controller.getDao();
        try {
            subTaskCalc = new LoadRateCalculator(dataReader.getTotalRows());
        } catch (DataAccessObjectException e) {
            throw new DataAccessObjectInitializationException(e.getMessage(), e);
        }
        successWriter = getSuccessWriter(config.getBoolean(Config.WRITE_UTF8));
        errorWriter = getErrorWriter(config.getBoolean(Config.WRITE_UTF8));
        setVisitor();
    }

    /**
     * @return Error Writer
     * @throws DataAccessObjectInitializationException
     */
    protected DataWriter getErrorWriter(boolean writeUtf8) throws DataAccessObjectInitializationException {
        String filename = config.getString(Config.OUTPUT_ERROR);
        if (filename == null || filename.length() == 0) { throw new DataAccessObjectInitializationException(Messages
                .getString("LoadAction.errorMissingErrorFile")); }
        // TODO: Make sure that specific DAO is not mentioned: use DataReader, DataWriter, or DataAccessObject
        return (new CSVFileWriter(filename, writeUtf8, true));
    }

    /**
     * @return Success Writer
     * @throws DataAccessObjectInitializationException
     */
    protected DataWriter getSuccessWriter(boolean writeUtf8) throws DataAccessObjectInitializationException {
        String filename = config.getString(Config.OUTPUT_SUCCESS);
        if (filename == null || filename.length() == 0) { throw new DataAccessObjectInitializationException(Messages
                .getString("LoadAction.errorMissingSuccessFile")); }
        // TODO: Make sure that specific DAO is not mentioned: use DataReader, DataWriter, or DataAccessObject
        return (new CSVFileWriter(filename, writeUtf8, true));
    }

    /**
     * This method should set the visitor for the load action.
     */
    protected abstract void setVisitor();

    /**
     * This is the main method. Call this method to execute the action
     * 
     * @throws DataAccessObjectException
     * @throws LoadException
     */
    public void execute() throws DataAccessObjectException, LoadException {

        try {

            logger.info(getMessage("loading", config.getString(Config.OPERATION)));
            this.subTaskCalc.setSuccessErrorIsKnown(getSuccessErrorIsKnown());
            subTaskCalc.start();
            // open the dao
            dataReader.open();
            // set the starting row
            rowToStart();
            // start the Progress Monitor
            //monitor.beginTask(getMessage("loading", config.getString(Config.OPERATION)), dataReader.getTotalRows());

            List<String> columnNames = dataReader.getColumnNames();
            openSuccessWriter(columnNames);
            openErrorWriter(columnNames);

            // begin the while loop
            wasCancelled = false;
            boolean moreDaoRows = true;
            while (moreDaoRows) {
                moreDaoRows = visitRowList();
            }

            // load any remaining rows
            visitor.flushRemaining();

            logger.info(getMessage("logSuccess")); //$NON-NLS-1$

            Object[] args = { String.valueOf(visitor.getNumSuccess()), config.getString(Config.OPERATION),
                    String.valueOf(visitor.getNumErrors()) };

            // need to close the streams before displaying the success message
            closeAll();

            // set the monitor to done
            if (wasCancelled) {
               // monitor.doneSuccess(getMessage("cancel", args));
            } else {
                //monitor.doneSuccess(getMessage("success", args));
            }
        } catch (ApiFault e) {
            logger.fatal(getMessage("loadException"), e); //$NON-NLS-1$
          //  monitor.doneError(e.getExceptionMessage()); //$NON-NLS-1$
        } catch (Exception e) {
            logger.fatal(getMessage("loadException"), e); //$NON-NLS-1$
          //  monitor.doneError(e.getMessage()); //$NON-NLS-1$
        } finally {
            closeAll();
        }
    }

    private void closeAll() {
        dataReader.close();
        successWriter.close();
        errorWriter.close();
    }

    protected abstract boolean getSuccessErrorIsKnown();

    /**
     * @return true if there're still more rows, false if done
     * @throws DataAccessObjectException
     * @throws ParameterLoadException
     * @throws LoadException
     * @throws ConnectionException
     */
    protected boolean visitRowList() throws DataAccessObjectException, ParameterLoadException, LoadException,
            ConnectionException {
        int loadBatchSize = this.config.getLoadBatchSize();
        List<Map<String, Object>> daoRowList = dataReader.readRowList(loadBatchSize);
        if (daoRowList == null || daoRowList.size() == 0) { return false; }
        for (Map<String, Object> daoRow : daoRowList) {
            if (!DAORowUtil.isValidRow(daoRow)) { return false; }

            visitor.visit(daoRow);

            // check if the monitor was cancelled
           /* if (monitor.isCanceled()) {
                logger.info(getMessage("logCanceled")); //$NON-NLS-1$
                wasCancelled = true;
                return false;
            }*/
        }
        return true;
    }

    protected void openSuccessWriter(List<String> header) throws LoadException {
        List<String> headerSuccess = new LinkedList<String>(header);

        // add the ID column if not there already
        if (!Config.ID_COLUMN_NAME.equals(headerSuccess.get(0))) {
            headerSuccess.add(0, Config.ID_COLUMN_NAME);
        }
        headerSuccess.add(Config.STATUS_COLUMN_NAME);
        try {
            successWriter.open();
            successWriter.setColumnNames(headerSuccess);
        } catch (DataAccessObjectInitializationException e) {
            throw new LoadException(getMessage("errorOpeningSuccessFile", config.getString(Config.OUTPUT_SUCCESS)), e);
        }
    }

    protected void openErrorWriter(List<String> header) throws LoadException {
        List<String> headerError = new LinkedList<String>(header);

        // add the ERROR column
        headerError.add(Config.ERROR_COLUMN_NAME);
        try {
            errorWriter.open();
            errorWriter.setColumnNames(headerError);
        } catch (DataAccessObjectInitializationException e) {
            throw new LoadException(getMessage("errorOpeningErrorFile", config.getString(Config.OUTPUT_ERROR)), e);
        }
    }

    /**
     * Set the dataReader to point to the row where load has to be started
     * 
     * @throws LoadException
     */
    protected void rowToStart() throws LoadException {
        // start at the correct row
        config.setValue(LastRun.LAST_LOAD_BATCH_ROW, 0);
        int rowToStart;
        try {
            rowToStart = config.getInt(Config.LOAD_ROW_TO_START_AT);
        } catch (ParameterLoadException e) {
            // if can't load rowToStart, just start from the beginning
            rowToStart = 0;
        }
        if (rowToStart > 0) {
            Map<String, Object> row;
            int currentRow = dataReader.getCurrentRowNumber();
            while (currentRow < rowToStart) {
                try {
                    row = dataReader.readRow();
                } catch (DataAccessObjectException e) {
                    String errMsg = Messages.getString("LoadAction.errorDaoStartRow");
                    logger.error(errMsg, e); //$NON-NLS-1$
                    throw new LoadException(errMsg, e);
                }
                if (!DAORowUtil.isValidRow(row)) break;

                currentRow = dataReader.getCurrentRowNumber();
                if (currentRow > 0) {
                  //  monitor.worked(currentRow);
                }
            }
            // set the last processed value to the starting row
            config.setValue(LastRun.LAST_LOAD_BATCH_ROW, rowToStart);
            try {
                config.saveLastRun();
            } catch (IOException e) {
                String errMsg = Messages.getString("LoadAction.errorLastRun");
                logger.error(errMsg, e); //$NON-NLS-1$
                throw new LoadException(errMsg, e);
            }
        }
    }

    protected String getMessage(String key, Object... args) {
        final String msg = Messages.getMessage(getClass(), key, true, args);
        return msg == null ? Messages.getMessage("LoadAction", key, args) : msg;
    }

}
