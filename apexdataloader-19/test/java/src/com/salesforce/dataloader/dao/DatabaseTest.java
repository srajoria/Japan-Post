/*
 * Copyright (c) 2006, salesforce.com, inc.
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
package com.salesforce.dataloader.dao;

import java.util.*;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.TestBase;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.database.DatabaseReader;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;
import com.salesforce.dataloader.util.AccountRowComparator;
import com.salesforce.dataloader.util.DatabaseTestUtil;

/**
 * Unit test for database operations
 *
 * @author awarshavsky
 * @since before
 */
public class DatabaseTest extends TestBase {

    public DatabaseTest(String name) {
        super(name);
    }

    // logger
    private static Logger logger = Logger.getLogger(DatabaseReader.class);

    private static final String[] VALIDATE_COLS = new String[] {DatabaseTestUtil.EXT_ID_COL, DatabaseTestUtil.SFDC_ID_COL, DatabaseTestUtil.NAME_COL, DatabaseTestUtil.PHONE_COL, DatabaseTestUtil.REVENUE_COL, DatabaseTestUtil.ACCOUNT_NUMBER_COL};
    private static final int NUM_ROWS = 10000;

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

    public void testDatabaseInsertQuery() {
        // insert some data
        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), true/*insert*/, NUM_ROWS);

        // query and verify the results
        verifyDbInsertOrUpdate(getController(), true, true);
    }

    public void testDatabaseUpdateQuery() {
        // insert some data
        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), true/*insert*/, NUM_ROWS);

        // update some data
        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), false/*update*/, NUM_ROWS);

        // verify update
        verifyDbInsertOrUpdate(getController(), false, true);
    }

    public void testDatabaseDateMappingDate() {
        doTestDatabaseDateMapping(DatabaseTestUtil.DateType.DATE, true);
    }

    public void testDatabaseDateMappingCalendar() {
        doTestDatabaseDateMapping(DatabaseTestUtil.DateType.CALENDAR, true);
    }

    public void testDatabaseDateMappingString() {
        doTestDatabaseDateMapping(DatabaseTestUtil.DateType.STRING, true);
    }

    public void testDatabaseDateMappingNull() {
        // just make sure that this works.  Used to crash.
        doTestDatabaseDateMapping(DatabaseTestUtil.DateType.NULL, false);
    }

    public void doTestDatabaseDateMapping(DatabaseTestUtil.DateType dateType, boolean verifyDates) {
        // insert some data
        DatabaseTestUtil.insertOrUpdateAccountsDb(getController(), true/*insert*/, 100, dateType);

        // query and verify the results
        verifyDbInsertOrUpdate(getController(), true, verifyDates);
    }

    /**
     * @param theController
     * @param validateDates TODO
     *
     */
    private static void verifyDbInsertOrUpdate(Controller theController, boolean isInsert, boolean validateDates) {
        DatabaseReader reader = null;
        logger.info("Verifying database success for '" + (isInsert ? "insert" : "update") + "' operation");
        try {
            // sort order is reverse between insert and update
            reader = new DatabaseReader(theController.getConfig(), "queryAccountAll");
            reader.open();
            int readBatchSize = 1000;
            List<Map<String,Object>> readRowList = reader.readRowList(readBatchSize);
            int rowsProcessed = 0;
            assertNotNull("Error reading " + readBatchSize + " rows", readRowList);
            while(readRowList.size() > 0) {
                // sort the row list so that it comes out in the right order.
                // for the update, the order is reversed.
                Collections.sort(readRowList, new AccountRowComparator(!isInsert));
                logger.info("Verifying database success for next " + (rowsProcessed + readRowList.size()) + " rows");
                for (int i=0; i < readRowList.size(); i++) {
                    Map<String,Object> readRow = readRowList.get(i);
                    assertNotNull("Error reading data row #" + i + ": the row shouldn't be null", readRow);
                    assertTrue("Error reading data row #" + i + ": the row shouldn't be empty", readRow.size()>0);
                    Map<String,Object> expectedRow = DatabaseTestUtil.getInsertOrUpdateAccountRow(isInsert, rowsProcessed, DatabaseTestUtil.DateType.VALIDATION);
                    // verify all expected data
                    for(String colName : VALIDATE_COLS) {
                        verifyCol(colName, readRow, expectedRow);
                    }
                    if(validateDates) {
                        verifyCol(DatabaseTestUtil.LAST_UPDATED_COL, readRow, expectedRow);
                    }
                    rowsProcessed++;
                }
                readRowList = reader.readRowList(readBatchSize);
                assertNotNull("Error reading " + readBatchSize + " rows", readRowList);
            }
        } catch (DataAccessObjectInitializationException e) {
            fail("Error initializing database reader for db config: " + "queryAccountAll");
        } catch (DataAccessObjectException e) {
            fail("Error getting database from the database using db config: "+ "queryAccountAll");
        } finally {
            if(reader != null) reader.close();
        }
    }

    /**
     * @param colName
     * @param col
     * @param colExpected
     */
    private static void verifyCol(String colName, Map<String,Object> row, Map<String,Object> expectedRow) {
        assertNotNull("Column: " + colName + " value should not be null :", row.get(colName));
        assertEquals("Data validation failed for column: " + colName +
                ", expected type: " + expectedRow.get(colName).getClass().getName() +
                ", actual type: " + row.get(colName).getClass().getName(),
                expectedRow.get(colName), row.get(colName));
    }
}
