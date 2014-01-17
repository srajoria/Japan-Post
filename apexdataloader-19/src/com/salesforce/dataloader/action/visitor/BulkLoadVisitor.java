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

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.beanutils.DynaBean;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataReader;
import com.salesforce.dataloader.dao.DataWriter;
import com.salesforce.dataloader.dyna.DateConverter;
import com.salesforce.dataloader.exception.*;
import com.salesforce.dataloader.util.LoadRateCalculator;
import com.sforce.async.*;
import com.sforce.ws.ConnectionException;

/**
 * 
 * Visitor for operations using the bulk API client
 *
 * @author Jesper Joergensen, Colin Jarvis
 * @since 162
 */
public class BulkLoadVisitor extends DAOLoadVisitor {

    private static final String SUCCESS_RESULT_COL = "Success";
    private static final String ERROR_RESULT_COL = "Error";
    private static final String ID_RESULT_COL = "Id";
    private static final String CREATED_RESULT_COL = "Created";
    private static final String ENCODING = "UTF-8";

    private static final DateFormat DATE_FMT;
    static {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(DateConverter.GMT);
        DATE_FMT = dateFormat;
    }

    private final List<Set<String>> explicitNulls = new ArrayList<Set<String>>();
    private final boolean isDelete;
    private final long checkStatusInterval;

    public BulkLoadVisitor(Controller controller, DataWriter successWriter,
            DataWriter errorWriter, LoadRateCalculator subTaskCalc) {
        super(controller, successWriter, errorWriter, subTaskCalc);
        long checkStatusInt = Config.DEFAULT_BULK_API_CHECK_STATUS_INTERVAL;
        try {
            checkStatusInt = this.config.getLong(Config.BULK_API_CHECK_STATUS_INTERVAL);
        } catch (ParameterLoadException e) {}
        this.checkStatusInterval = checkStatusInt;
        this.isDelete = controller.getConfig().getOperationInfo() .isDelete();
    }

    private JobState jobState;
    private JobInfo jobInfo;
    private long lastcheck;
    private int rowsProcessed;

    @Override
    protected void loadBatch() throws DataAccessObjectException, LoadException {
        try {
            createJob();
            createBatches();
            clearArrays();
         //   if (this.monitor.isCanceled()) abort();
        } catch (AsyncApiException e) {
            handleException(e);
        } catch (ConnectionException e) {
            handleException(e);
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Throws a load exception
     */
    @Override
    protected void handleException(Throwable t) throws LoadException {
        try {
            abort();
        } catch (AsyncApiException e) {} catch (ConnectionException e) {}
        handleException(getOverrideMessage(t), t);
    }

    private String getOverrideMessage(Throwable t) {
        if (t instanceof AsyncApiException) {

            final AsyncApiException aae = (AsyncApiException)t;
            final String hardDeleteNoPermsMessage = "hardDelete operation requires special user profile permission, please contact your system administrator";

            if (aae.getExceptionCode() == AsyncExceptionCode.FeatureNotEnabled
                    && aae.getExceptionMessage().contains(hardDeleteNoPermsMessage)) { return Messages.getMessage(
                    getClass(), "hardDeleteNoPerm"); }
        }
        return null;
    }

    private void createBatches() throws LoadException, IOException, AsyncApiException, ConnectionException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os, true, ENCODING);
        doOneBatch(out, os, this.dynaArray, this.explicitNulls);
    }

    private void doOneBatch(PrintStream out, ByteArrayOutputStream os, List<DynaBean> rows, List<Set<String>> nulls)
            throws IOException, LoadException, AsyncApiException, ConnectionException {
        int recordsInBatch = 0;
        assert rows.size() == nulls.size();
        final List<String> userColumns = this.controller.getDao().getColumnNames();
        List<String> headerColumns = null;
        for (int i = 0; i < rows.size(); i++) {
            final DynaBean row = rows.get(i);
            final Set<String> nullsForRow = nulls.get(i);

            if (recordsInBatch == 0) headerColumns = addHeader(out, os, row, userColumns);
            writeRow(row, out, os, recordsInBatch, nullsForRow, headerColumns);
            recordsInBatch++;

            if (os.size() > Config.MAX_BULK_API_BATCH_BYTES) {
                createBatch(os, recordsInBatch); // resets outputstream

                recordsInBatch = 0;
            }
        }
        createBatch(os, recordsInBatch);
        periodicCheckStatus();
    }

    private void writeRow(DynaBean row, PrintStream out, ByteArrayOutputStream os, int recordsInBatch,
            Set<String> nullsForRow, List<String> header) throws IOException, LoadException {
        boolean notFirst = false;
        for (String column : header) {
            if (notFirst)
                out.print(',');
            else
                notFirst = true;

            writeSingleColumn(out, nullsForRow, column, row.get(column));
        }
        out.println();
    }

    private void writeSingleColumn(PrintStream out, Set<String> nullsForRow, String fieldName, Object fieldValue)
            throws LoadException {

        if (fieldValue != null) {
            out.print('"');
            if (fieldValue instanceof Calendar) {
                out.print(DATE_FMT.format(((Calendar)fieldValue).getTime()));
            } else if (fieldValue instanceof byte[]) {
                throw new LoadException(Messages.getMessage("FinishPage", "cannotMapBase64ForBulkApi", fieldName));
                // TODO: this is not currently supported in bulk api
                // out.write(Base64.encode((byte[])fieldValue));
            } else {
                // escape quotes: " -> ""
                out.print(fieldValue.toString().replace("\"", "\"\""));
            }
            out.print('"');
        } else if (nullsForRow.contains(fieldName) && !this.isDelete) {
            out.print("#N/A");
        } else {
            logger.debug("No value provided for field: " + fieldName);
        }
    }

    private List<String> addHeader(PrintStream out, ByteArrayOutputStream os, DynaBean row, List<String> columns)
            throws IOException, LoadException {
        boolean notFirst = false;
        List<String> cols = new ArrayList<String>();
        for (String userColumn : columns) {
            String sfdcColumn = this.controller.getMappingManager().getMappingFor(userColumn);
            // if the column is not mapped, don't send it
            if (sfdcColumn == null || sfdcColumn.length() == 0) {
                logger.warn("Cannot find mapping for column: " + userColumn + ".  Omitting column");
                continue;
            }
            if (this.isDelete && (notFirst || !"id".equalsIgnoreCase(sfdcColumn))) { throw new LoadException(Messages
                    .getMessage(getClass(), "deleteCsvError")); }
            if (notFirst)
                out.print(',');
            else
                notFirst = true;
            out.print(sfdcColumn.replace(':', '.'));
            cols.add(sfdcColumn);
        }
        out.println();
        logger.debug(Messages.getMessage(getClass(), "logBatchHeader", new String(os.toByteArray(), ENCODING)));
        return Collections.unmodifiableList(cols);
    }

    private void createJob() throws LoadException, AsyncApiException, ConnectionException {
        if (this.jobInfo == null) this.jobInfo = createJobInfo();
        if (this.jobState == null) this.jobState = JobState.create(this.jobInfo.getId());
    }

    private JobInfo createJobInfo() throws AsyncApiException, ConnectionException {
        JobInfo job = new JobInfo();
        final OperationEnum op = this.config.getOperationInfo().getOperationEnum();;
        job.setOperation(op);
        if (op == OperationEnum.upsert) job.setExternalIdFieldName(this.config.getString(Config.EXTERNAL_ID_FIELD));
        job.setObject(this.config.getString(Config.ENTITY));
        job.setContentType(ContentType.CSV);
        job.setConcurrencyMode(this.config.getBoolean(Config.BULK_API_SERIAL_MODE) ? ConcurrencyMode.Serial
                : ConcurrencyMode.Parallel);
        String assRule = this.config.getString(Config.ASSIGNMENT_RULE);
        if (assRule != null && assRule.length() == 15 || assRule.length() == 18) {
            job.setAssignmentRuleId(assRule);
        }

        job = this.controller.getBulkClient().getClient().createJob(job);
        logger.info(Messages.getMessage(getClass(), "logJobCreated", job.getId()));
        return job;
    }

    private void createBatch(ByteArrayOutputStream os, int numRecords) throws AsyncApiException, ConnectionException,
            LoadException {
        if (numRecords <= 0) return;
        byte[] request = os.toByteArray();
        os.reset();
        final RestConnection rest = this.controller.getBulkClient().getClient();
        final BatchInfo batch = rest.createBatchFromStream(this.jobInfo, new ByteArrayInputStream(request, 0,
                request.length));
        logger.info(Messages.getMessage(getClass(), "logBatchLoaded", batch.getId(), numRecords));
        this.jobState.addBatchInfo(batch, numRecords);
    }

    private void periodicCheckStatus() throws AsyncApiException, ConnectionException {
        if (System.currentTimeMillis() - this.lastcheck > checkStatusInterval) {
            logger.info(Messages.getMessage(getClass(), "logCheckStatus"));
            this.jobInfo = this.controller.getBulkClient().getClient().getJobStatus(this.jobInfo.getId());
            int newRowsProcessed = this.jobInfo.getNumberRecordsProcessed();
            int recentlyProcessedRows = newRowsProcessed - this.rowsProcessed;
          //  this.monitor.worked(recentlyProcessedRows);
           // this.monitor.setSubTask(this.subTaskCalc.calculateSubTask(recentlyProcessedRows, newRowsProcessed, 0, 0));
            this.rowsProcessed = newRowsProcessed;
            this.lastcheck = System.currentTimeMillis();
        }
    }

    @Override
    public void flushRemaining() throws LoadException, DataAccessObjectException {
        super.flushRemaining();
        try {
            closeJobAndAwaitCompletion();
            getResults();
        } catch (AsyncApiException e) {} catch (ConnectionException e) {} catch (IOException e) {}
    }

	private void closeJobAndAwaitCompletion() throws AsyncApiException,
			ConnectionException, LoadException {
		if (this.jobInfo == null) {
			logger.info(Messages.getMessage(getClass(), "jobNotCreated"));
			return;
		}
		this.controller.getBulkClient().getClient().closeJob(
				this.jobInfo.getId());
		awaitJobCompletion();
	}

    private void awaitJobCompletion() throws ConnectionException, AsyncApiException, LoadException {
        getJobState("CheckStatus");
        final RestConnection rest = this.controller.getBulkClient().getClient();

        JobInfo job = this.jobInfo;
        while (job.getNumberBatchesQueued() > 0 || job.getNumberBatchesInProgress() > 0) {

            int newRecordsProcessed = job.getNumberRecordsProcessed();
            int numRecentlyProcessed = newRecordsProcessed - this.rowsProcessed;
          //  this.monitor.worked(numRecentlyProcessed);
          //  this.monitor.setSubTask(this.subTaskCalc.calculateSubTask(numRecentlyProcessed, newRecordsProcessed, 0, 0));
            this.rowsProcessed = newRecordsProcessed;
            logger.info(Messages.getMessage(getClass(), "logJobStatus", job.getNumberBatchesQueued(), job
                    .getNumberBatchesInProgress(), job.getNumberBatchesCompleted(), job.getNumberBatchesFailed()));
            try {
                Thread.sleep(checkStatusInterval);
            } catch (final InterruptedException e) {}
            // check if the monitor was cancelled
         /*   if (this.monitor.isCanceled()) {
                logger.info(Messages.getMessage(getClass(), "logCanceled")); //$NON-NLS-1$
                abort();
                return;
            }*/
            job = rest.getJobStatus(this.jobState.jobId);
        }
        job = rest.getJobStatus(this.jobState.jobId);
        this.jobInfo = job;

       // this.monitor.worked(job.getNumberRecordsProcessed() - this.rowsProcessed);
    }

    private void getJobState(String method) throws LoadException {
        if (this.jobState == null) {
            this.jobState = JobState.load();
            if (this.jobState == null) throw new LoadException(Messages.getMessage(getClass(), "noJobId", method));
        }
    }

    private void getResults() throws ConnectionException, AsyncApiException, LoadException, DataAccessObjectException,
            IOException {
       // this.monitor.setSubTask(Messages.getMessage(getClass(), "retrievingResults"));
        getJobState("getResults");
        final DataReader dataReader = (DataReader)this.controller.getDao();
        dataReader.close();
        // TODO: doing this causes sql to be executed twice
        dataReader.open();
        final RestConnection rest = this.controller.getBulkClient().getClient();
        final Map<String, BatchInfo> batchInfos = getBatchInfos();
        // For Bulk API, we don't save any success or error until the end,
        // so we have to go through the original CSV from the beginning while
        // we go through the results from the server.

        for (final BatchState bs : this.jobState.batches) {
            final List<Map<String, Object>> rows = dataReader.readRowList(bs.rowCount);
            final BatchInfo batchInfo = batchInfos.get(bs.id);
            final BatchStateEnum state = batchInfo.getState();
            final String stateMessage = Messages.getMessage(getClass(), "batchError", batchInfo.getStateMessage());
            final int recordsProcessed = batchInfo.getNumberRecordsProcessed();
            if (state == BatchStateEnum.Completed || recordsProcessed > 0) {

                final CSVReader r = new CSVReader(rest.getBatchResultStream(this.jobState.jobId, bs.id));
                final Map<String, Integer> idx = new HashMap<String, Integer>();
                final List<String> header = r.nextRecord();
                for (int i = 0; i < header.size(); i++) {
                    idx.put(header.get(i), i);
                }
                for (final Map<String, Object> row : rows) {
                    final List<String> res = r.nextRecord();
                    if (res == null) {
                        // no result for this column. In this case it failed, and we should use the batch state message
                        if (state == BatchStateEnum.Failed) {
                            row.put(Config.ERROR_COLUMN_NAME, stateMessage);
                            this.numErrors++;
                            this.errorWriter.writeRow(row);
                            
                        } else {
                            final String errMsg = Messages.getMessage(getClass(), "errorBadResults", bs.id);
                            logger.error(errMsg);
                            throw new LoadException(Messages.getMessage(getClass(), "errorBadResults", bs.id));
                        }
                    } else {
                        if (Boolean.valueOf(res.get(idx.get(SUCCESS_RESULT_COL)))) {
                            String successMessage;
                            switch (this.config.getOperationInfo()) {
                            case hard_delete:
                                successMessage = "statusItemHardDeleted";//$NON-NLS-1$
                                break;
                            case delete:
                                successMessage = "statusItemDeleted";//$NON-NLS-1$
                                break;
                            default:
                                successMessage = Boolean.valueOf(res.get(idx.get(CREATED_RESULT_COL))) ? "statusItemCreated"//$NON-NLS-1$
                                        : "statusItemUpdated"; //$NON-NLS-1$
                            }
                            row.put(Config.STATUS_COLUMN_NAME, Messages.getMessage(getClass(), successMessage));
                            row.put(Config.ID_COLUMN_NAME, res.get(idx.get(ID_RESULT_COL)));
                            this.numSuccess++;
                            this.successWriter.writeRow(row);
                        } else {
                            row.put(Config.ERROR_COLUMN_NAME, parseAsyncApiError(idx, res));
                            this.numErrors++;
                            this.errorWriter.writeRow(row);
                        }
                    }
                }
            } else {
                for (final Map<String, Object> row : rows) {
                    row.put(Config.ERROR_COLUMN_NAME, stateMessage);
                }
                this.numErrors += rows.size();
                this.errorWriter.writeRowList(rows);
            }
        }
        DataReader reader = (DataReader)controller.getDao();
        //monitor.setSubTask(subTaskCalc.calculateSubTask(numSuccess + numErrors, reader.getCurrentRowNumber(),
        //        numSuccess, numErrors));

        this.successWriter.close();
        this.errorWriter.close();
    }

	private String parseAsyncApiError(final Map<String, Integer> idx,
			final List<String> res) {
		final String sep = ":";
		final String suffix = "--";
		final String errString = res.get(idx.get(ERROR_RESULT_COL));
		final int lastSep = errString.lastIndexOf(sep);
		if (lastSep > 0 && errString.endsWith(suffix)) {
			String fields = errString.substring(lastSep + 1, errString.length()
					- suffix.length());
			String start = errString.substring(0, lastSep);
			if (fields != null && fields.length() > 0) {
				return new StringBuilder(start).append("\n").append(
						"Error fields: ").append(fields).toString();
			} else {
				return start;
			}
		} else {
			return errString;
		}
	}

    private Map<String, BatchInfo> getBatchInfos() throws AsyncApiException, ConnectionException {
        final BatchInfoList bil = this.controller.getBulkClient().getClient().getBatchInfoList(this.jobState.jobId);

        final Map<String, BatchInfo> batchInfos = new HashMap<String, BatchInfo>();
        for (final BatchInfo b : bil.getBatchInfo()) {
            logger.info(Messages.getMessage(getClass(), "logBatchInfo", b.getId(), b.getState(), b.getStateMessage()));
            batchInfos.put(b.getId(), b);
        }
        return batchInfos;
    }

    private void abort() throws AsyncApiException, ConnectionException {
        if (this.jobInfo != null && this.jobState != null) {
            //this.monitor.setSubTask(Messages.getMessage(getClass(), "abortingJob"));
            this.controller.getBulkClient().getClient().abortJob(this.jobInfo.getId());
            this.jobInfo = null;
            this.jobState = null;
        }
    }
    
    @Override
    protected void hook_preConvert(Map<String, Object> row)  {
        super.hook_preConvert(row);
        final HashSet<String> nullsForRow = new HashSet<String>();
        fixupRow(nullsForRow, row);
        this.explicitNulls.add(nullsForRow);
    }

    private void fixupRow(Set<String> nulls, Map<String, Object> row) {
        for (Map.Entry<String, Object> ent : row.entrySet()) {
            Object val = ent.getValue();
            if (val != null && "#N/A".equals(String.valueOf(val))) {
                nulls.add(ent.getKey());
            }
        }
        for (String key : nulls) {
            row.put(key, null);
        }
    }
    
    @Override
    public void clearArrays() {
        super.clearArrays();
        this.explicitNulls.clear();
    }
}
