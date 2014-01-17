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

package com.salesforce.dataloader.util;

import java.util.Date;

import com.salesforce.dataloader.config.Messages;

/**
 * Calculates of the progress strings
 *
 * @author lviripaeff
 * @since before
 */
public class LoadRateCalculator {

    Date lastLoadTime = null;
    Date startTime = null;
    int totalRecords = 0;
    boolean successErrorIsKnown = true;

    public LoadRateCalculator(int totalRecs) {
        totalRecords = totalRecs;
    }

    public void start() {
        lastLoadTime = new Date();
        startTime = new Date();
    }
    
    public void setSuccessErrorIsKnown(boolean value) {
        this.successErrorIsKnown = value;
    }

    public String calculateSubTask(int worked, int currentRecord, int numSuccess, int numErrors) {

        Date currentLoadTime = new Date();
        long currentPerMin = currentRecord * 60 * 60;
        long rate;


        long totalElapsed = currentLoadTime.getTime() - startTime.getTime();
        if (totalElapsed == 0) {
            rate = 0;
        } else {
            rate = currentPerMin / totalElapsed * 1000 ;
        }

        long percentCompleted;
        if (totalRecords == 0) {
            percentCompleted = 0;
        } else {
            percentCompleted = (currentRecord * 100) / (totalRecords);
        }

        long timeToCompleteMillis;
        if (percentCompleted == 0) {
            timeToCompleteMillis = 0;
        } else {
            timeToCompleteMillis = ((totalElapsed * 100) / percentCompleted) - totalElapsed;
        }
        long timeToCompleteMins = timeToCompleteMillis / (1000 * 60);

        long remainingSeconds = (timeToCompleteMillis / 1000) - (timeToCompleteMins * 60);

        String[] args = { String.valueOf(currentRecord), String.valueOf(totalRecords), String.valueOf(rate),
                String.valueOf(timeToCompleteMins), String.valueOf(remainingSeconds), String.valueOf(numSuccess),
                String.valueOf(numErrors) };

        if(successErrorIsKnown) {
            return Messages.getFormattedString("LoadRateCalculator.processed", args); //$NON-NLS-1$
        } else {
            return Messages.getFormattedString("LoadRateCalculator.processedSuccessErrorUnknown", args); //$NON-NLS-1$
        }
    }
}