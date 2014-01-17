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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.exception.LoadException;
import com.sforce.async.BatchInfo;

/**
 * 
 * Helper class to deal with saving and reading job state
 *
 * @author Jesper Joergensen, Colin Jarvis
 * @since 162
 */
class JobState {
    private static final String BATCH_DELIM = ",";
    private static final String BATCHES_PROP = "batches";
    private static final String ACTIVE_JOB_PROPERTIES_FILE = "active_job.properties";
    private static final String JOB_ID_PROP = "jobId";
    private static final Logger logger = Logger.getLogger(JobState.class);

    final String jobId;
    final List<BatchState> batches;

    private JobState(String jobId, List<BatchState> batches) {
        this.jobId = jobId;
        this.batches = batches;
    }

    static JobState create(String jobId) throws LoadException {
        final JobState jobState = new JobState(jobId, new ArrayList<BatchState>());
        jobState.save();
        return jobState;
    }

    void addBatchInfo(BatchInfo info, int size) throws LoadException {
        this.batches.add(new BatchState(info.getId(), size));
        save();
    }

    private void save() throws LoadException {
        final Properties p = new Properties();
        if (this.jobId != null) {
            p.setProperty(JOB_ID_PROP, this.jobId);
        }
        if (!this.batches.isEmpty()) {
            final StringBuilder b = new StringBuilder();
            for (final BatchState bs : this.batches.subList(0, this.batches.size() - 1)) {
                b.append(bs).append(BATCH_DELIM);
            }
            b.append(this.batches.get(this.batches.size() - 1));
            logger.debug(Messages.getMessage(getClass(), "debugSaveBatch", b));
            p.setProperty(BATCHES_PROP, b.toString());
        }
        try {
            final FileOutputStream out = new FileOutputStream(ACTIVE_JOB_PROPERTIES_FILE);
            try {
                p.store(out, null);
            } finally {
                out.close();
            }
        } catch (final IOException e) {
            handleException(e);
        }

    }

    static JobState load() throws LoadException {
        final Properties p = new Properties();
        try {
            final FileInputStream in = new FileInputStream(ACTIVE_JOB_PROPERTIES_FILE);
            try {
                p.load(in);
            } finally {
                in.close();
            }
        } catch (final FileNotFoundException e) {
            // no active job
            return null;
        } catch (final IOException e) {
            handleException(e);
        }

        final String jobId = p.getProperty(JOB_ID_PROP);
        // simple check for badly formatted ID
        if (jobId == null || jobId.length() != 18 && jobId.length() != 15) return null;

        final String[] arr = p.getProperty(BATCHES_PROP, "").split(BATCH_DELIM);
        final List<BatchState> batches = new ArrayList<BatchState>(arr.length);
        for (final String s : arr) {
            batches.add(new BatchState(s));
        }

        return new JobState(jobId, batches);
    }

    private static void handleException(Exception e) throws LoadException {
        logger.fatal(e);
        throw new LoadException(e);
    }

}