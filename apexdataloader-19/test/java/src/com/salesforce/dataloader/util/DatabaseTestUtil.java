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
package com.salesforce.dataloader.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.database.DatabaseWriter;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;

/**
 * Utilities for database connectivity testing
 *
 * @author awarshavsky
 * @since before
 */
public class DatabaseTestUtil {
    // logger
    private static Logger logger = Logger.getLogger(DatabaseTestUtil.class);
    
    public enum DateType {CALENDAR, DATE, STRING, VALIDATION, NULL};

    public static void insertOrUpdateAccountsDb(Controller theController, boolean isInsert, int numAccounts) {
        insertOrUpdateAccountsDb(theController, isInsert, numAccounts, DateType.CALENDAR);
    }
    
    /**
     * @param theController
     * @param isInsert
     * @param numAccounts
     */
    public static void insertOrUpdateAccountsDb(Controller theController, boolean isInsert, int numAccounts, DateType dateType) {
        DatabaseWriter writer = null;
        String dbConfigName = isInsert ? "insertAccount" : "updateAccount";
        logger.info("Preparing to write " + numAccounts + " accounts to the database using db config: " + dbConfigName);
        try {
            writer = new DatabaseWriter(theController.getConfig(), dbConfigName);
            writer.open();
            List<Map<String,Object>> accountRowList = new ArrayList<Map<String,Object>>();
            int rowsProcessed = 0;
            for(int i=0; i < numAccounts; i++) {
                Map<String,Object> accountRow = getInsertOrUpdateAccountRow(isInsert, i, dateType);
                accountRowList.add(accountRow);
                if(accountRowList.size() >= 1000 || i == (numAccounts-1)) {
                    rowsProcessed += accountRowList.size();
                    writer.writeRowList(accountRowList);
                    logger.info("Written " + rowsProcessed + " of " + numAccounts + " total accounts using database config: " + dbConfigName);
                    accountRowList = new ArrayList<Map<String,Object>>();
                }
            }
        } catch (DataAccessObjectInitializationException e) {
            TestCase.fail("Error initializing database writer for db config: " + dbConfigName + ", error: " + e.toString());
        } catch (DataAccessObjectException e) {
            String dbOperName = isInsert ? "inserting" : "updating";
            TestCase.fail("error " + dbOperName + " accounts to the database using db config: " + dbConfigName + ", error: " + e.toString());
        } finally {
            if(writer != null) writer.close();
        }
    }

    /**
     * Generate data for one account row based on the seqNum passed in. If insert is desired, text data is based on
     * seqNum, if update, text data is based on 9999-seqNum
     * 
     * @param isInsert
     *            if true, account is for insert, otherwise - for update
     * @param seqNum
     *            Account sequence in set of generated accounts
     * @param dateType Type for the date field values
     * @return Map<String,Object> containing account data based on seqNum
     */
    public static Map<String, Object> getInsertOrUpdateAccountRow(boolean isInsert, int seqNum, DateType dateType) {
        Map<String,Object> row = new HashMap<String,Object>();
        String operation;
        int seqInt;
        // external id is the key, use normal sequencing for update so the same set of records gets updated as inserted
        row.put(EXT_ID_COL, "1-" + String.format("%06d", seqNum));
        if(isInsert) {
            // for insert use "forward" sequence number for data
            seqInt = seqNum;
            operation = "insert";
        } else {
            // for update use "reverse" sequence number for data
            seqInt = 999999 - seqNum; 
            operation = "update";
        }
        String seqStr = String.format("%06d", seqInt);
        row.put(NAME_COL, "account " + operation + "#" + seqStr); // this is important to get the correct sort order
        row.put(PHONE_COL, "415-555-" + seqStr);
        row.put(SFDC_ID_COL, "001account_" + seqStr);
        row.put(REVENUE_COL, BigDecimal.valueOf(seqInt*1000));
        row.put(ACCOUNT_NUMBER_COL, "ACCT" + seqStr);
        Object dateValue;
        Calendar cal = Calendar.getInstance();
        // set the time a function of a given date so that the value is predictable
        cal.set(seqNum % 100 + 1900, 2, 4, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        switch(dateType) {
        case STRING:
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss'Z'Z");
            formatter.setCalendar(cal);
            dateValue = formatter.format(cal.getTime());
            break;
        case DATE:
            dateValue = cal.getTime();
            break;
        case NULL:
            dateValue = null;
            break;
        case VALIDATION:
            dateValue = new java.sql.Date(cal.getTimeInMillis());
            break;
        case CALENDAR:
        default:
            dateValue = cal;
            break;
        }
        row.put(LAST_UPDATED_COL, dateValue);
        return row;
    }

    /**
     * Delete all accounts from account table. Useful as a cleanup step
     */
    public static void deleteAllAccountsDb(Controller theController) {
        DatabaseWriter writer = null;
        try {
            writer = new DatabaseWriter(theController.getConfig(), "deleteAccountAll");
            writer.open();
            logger.info("Deleting all Accounts from database, using configuration: " + "deleteAccountAll");
            writer.writeRow(null);
        } catch (DataAccessObjectInitializationException e) {
            TestCase.fail("Error initializing database writer for db config: " + "deleteAccountAll");
        } catch (DataAccessObjectException e) {
            TestCase.fail("error deleting accounts from the database using db config: " + "deleteAccountAll");
        } finally {
            if(writer != null) writer.close();
        }
    }

    public static final String NAME_COL = "account_name";
    public static final String PHONE_COL = "business_phone";
    public static final String EXT_ID_COL = "account_ext_id";
    public static final String SFDC_ID_COL = "sfdc_account_id";
    public static final String REVENUE_COL = "annual_revenue";
    public static final String LAST_UPDATED_COL = "last_updated";
    public static final String ACCOUNT_NUMBER_COL = "account_number";

}
