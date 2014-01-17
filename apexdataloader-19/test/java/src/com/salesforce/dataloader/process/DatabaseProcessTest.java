/*
 * Copyright (c) 2006, salesforce.com, inc. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met: Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in
 * binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. Neither the name of salesforce.com, inc. nor the
 * names of its contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.dataloader.process;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.ConfigGenerator;
import com.salesforce.dataloader.ConfigTestSuite;
import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.LastRun;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.database.DatabaseReader;
import com.salesforce.dataloader.exception.*;
import com.salesforce.dataloader.util.DatabaseTestUtil;
import com.sforce.ws.ConnectionException;

/**
 * Automated tests for dataloader database batch interface
 *
 * @author awarshavsky
 * @since before
 */
public class DatabaseProcessTest extends ProcessTestBase {

    public static TestSuite suite() {
        return ConfigTestSuite.createSuite(DatabaseProcessTest.class);
    }

    public static ConfigGenerator getConfigGenerator() {
        final ConfigGenerator parentGen = ProcessTestBase.getConfigGenerator();

        final ConfigGenerator withoutBulkApi = new ConfigSettingGenerator(parentGen, Config.USE_BULK_API, Boolean.FALSE
                .toString());

        final ConfigGenerator withBulkApi = ConfigSettingGenerator.getBooleanGenerator(new ConfigSettingGenerator(
                parentGen, Config.USE_BULK_API, Boolean.TRUE.toString()), Config.BULK_API_SERIAL_MODE);
        return new UnionConfigGenerator(withoutBulkApi, withBulkApi);
    }

    // logger
    private static Logger logger = Logger.getLogger(DatabaseReader.class);
    private static final int NUM_ROWS = 1000;
    private static final int BATCH_SIZE = 100;

    public DatabaseProcessTest(String name, Map<String, String> config) {
        super(name, config);
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() {
        super.setUp();
        // delete accounts from database to start fresh
        DatabaseTestUtil.deleteAllAccountsDb(getController());
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        // delete accounts from database to finish with no leftovers
        DatabaseTestUtil.deleteAllAccountsDb(getController());
    }

    public void testExtractAccountDb() throws ConnectionException {
        // upsert accounts into salesforce so there's something to query
        upsertSfdcAccounts(NUM_ROWS);

        String processName = baseName + "Process";
        // do insert
        doExtractAccountDb(processName, NUM_ROWS, 0, true);
        // do update
        doExtractAccountDb(processName, NUM_ROWS, 0, false);
    }

    public void testExtractAccountDbNegative() throws ConnectionException {
        // upsert accounts into salesforce so there's something to query
        upsertSfdcAccounts(500);
        // upsert one bad record causing only one of the database write batches to fail
        upsertBadSfdcAccounts(1, 250);

        // do insert
        doExtractAccountDb("extractAccountDbProcess", 400, 100, true);
    }

    public void testUpsertAccountDb() {
        String processName = baseName + "Process";
        String startTime = "2006-01-01T00:00:00.000-0700";

        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), true, NUM_ROWS);

        // specify the name of the configured process and select appropriate database access type
        Map<String, String> argMap = getTestConfig();
        argMap.put(ProcessRunner.PROCESS_NAME, processName);
        try {
            Config.DATE_FORMATTER.parse(startTime);
        } catch (ParseException e) {
            fail("Cannot parse date string: " + startTime + ", error: " + e.getMessage());
        }
        argMap.put(LastRun.LAST_RUN_DATE, startTime);

        try {
            ProcessRunner runner = ProcessRunner.getInstance(argMap);
            runner.run();
            Controller theController = runner.getController(); // get controller created for this process

            // verify there were no errors during extract
            verifyNoError(theController);

            // verify there were no errors during extract
            verifyAllSuccess(theController);

        } catch (ProcessInitializationException e) {
            fail("Error initializing the process, error: " + e.getMessage());
        } catch (RuntimeException e) {
            fail("Exception has been caught, error: " + e.getMessage());
        }
    }

    public void testMaximumBatchRowsDb() {
        final Map<String, String> argMap = getTestConfig();
        final int numRows = Boolean.valueOf(argMap.get(Config.USE_BULK_API)) ? Config.MAX_BULK_API_BATCH_SIZE
                : Config.MAX_LOAD_BATCH_SIZE;

        final String processName = baseName + "Process";
        final String startTime = "2006-01-01T00:00:00.000-0700";

        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), true, numRows);

        // specify the name of the configured process and select appropriate database access type
        argMap.put(ProcessRunner.PROCESS_NAME, processName);
        try {
            Config.DATE_FORMATTER.parse(startTime);
        } catch (ParseException e) {
            fail("Cannot parse date string: " + startTime + ", error: " + e.getMessage());
        }
        argMap.put(LastRun.LAST_RUN_DATE, startTime);
        argMap.put(Config.LOAD_BATCH_SIZE, Integer.toString(numRows));

        try {
            final ProcessRunner runner = ProcessRunner.getInstance(argMap);
            runner.run();
            Controller theController = runner.getController(); // get controller created for this process

            // verify there were no errors during extract
            verifyNoError(theController);

            // verify there were no errors during extract
            verifyAllSuccess(theController);

        } catch (ProcessInitializationException e) {
            fail("Error initializing the process, error: " + e.getMessage());
        } catch (RuntimeException e) {
            fail("Exception has been caught, error: " + e.getMessage());
        }
    }


    protected void doExtractAccountDb(String processName, int expectedSuccesses, int expectedFailures, boolean isInsert) {

        // specify the name of the configured process and select appropriate database access type
        OperationInfo op = isInsert ? OperationInfo.insert : OperationInfo.update;
        Map<String, String> argMap = getTestConfig();
        argMap.put(ProcessRunner.PROCESS_NAME, processName);
        argMap.put(Config.DAO_NAME, op.name() + "Account");
        argMap.put(Config.OUTPUT_SUCCESS, new File(getTestStatusDir(), baseName + op.name() + "Success.csv")
                .getAbsolutePath());
        argMap.put(Config.OUTPUT_ERROR, new File(getTestStatusDir(), baseName + op.name() + "Error.csv")
                .getAbsolutePath());

        argMap.put(Config.DAO_WRITE_BATCH_SIZE, String.valueOf(BATCH_SIZE));

        try {
            Date startTime = new Date();

            ProcessRunner runner = ProcessRunner.getInstance(argMap);
            runner.run();
            Controller theController = runner.getController(); // get controller created for this process

            // verify there were no errors during extract
            verifyErrors(theController, expectedFailures);

            // verify there were no errors during extract
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("compare_date", startTime);
            verifyDbSuccess(theController, "queryAccountSince", params, expectedSuccesses);

        } catch (ProcessInitializationException e) {
            fail("Error initializing the process, error: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
            // fail("Exception has been caught, error: " + e.getMessage());
        }
    }

    /**
     * @param theController
     * @param startTime
     */
    private void verifyDbSuccess(Controller theController, String dbConfigName, Map<String,Object> params, int expectedSuccesses) {
        DatabaseReader reader = null;
        logger.info("Verifying database success for database configuration: " + dbConfigName);
        try {
            reader = new DatabaseReader(theController.getConfig(), dbConfigName);
            reader.open(params);
            int readBatchSize = theController.getConfig().getInt(Config.DAO_READ_BATCH_SIZE);
            List<Map<String,Object>> successRows = reader.readRowList(readBatchSize);
            int rowsProcessed = 0;
            assertNotNull("Error reading " + readBatchSize + " rows", successRows);
            while(successRows.size() > 0) {
                rowsProcessed += successRows.size();
                logger.info("Verifying database success for next " + successRows.size() + " of total " + rowsProcessed + " rows");
                assertTrue("No updated rows have been found in the database.", successRows.size() > 0);
                successRows = reader.readRowList(readBatchSize);
            }
            assertEquals(expectedSuccesses, rowsProcessed);
        } catch (DataAccessObjectInitializationException e) {
            fail("Error initializing database operation success verification using dbConfig: " + dbConfigName +
                    ", error:" + e.getMessage());
        } catch (DataAccessObjectException e) {
            fail("Error reading rows during database operation success verification using dbConfig: " + dbConfigName +
                    ", error:" + e.getMessage());
        } catch (ParameterLoadException e) {
            fail("Error getting a config parameter: " + e.getMessage()
                    + "during database operation success verification using dbConfig: " + dbConfigName);
        } finally {
            if(reader != null) reader.close();
        }
    }
}
