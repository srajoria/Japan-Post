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
package com.salesforce.dataloader.process;

import java.io.File;
import java.util.Map;

import junit.framework.TestSuite;

import com.salesforce.dataloader.ConfigGenerator;
import com.salesforce.dataloader.ConfigTestSuite;
import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.csv.CSVFileReader;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.ProcessInitializationException;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

/**
 * Test for dataloader batch interface, also known as "integration framework"
 *
 * @author awarshavsky
 * @since before
 */

public class CsvProcessTest extends ProcessTestBase {

	public static TestSuite suite() {
		return ConfigTestSuite.createSuite(CsvProcessTest.class);
	}

	public static ConfigGenerator getConfigGenerator() {
		final ConfigGenerator parentGen = ProcessTestBase.getConfigGenerator();
		final ConfigGenerator withoutBulkApi = new ConfigSettingGenerator(
				parentGen, Config.USE_BULK_API, Boolean.FALSE.toString());
		final ConfigGenerator withBulkApi = ConfigSettingGenerator
				.getBooleanGenerator(new ConfigSettingGenerator(parentGen,
						Config.USE_BULK_API, Boolean.TRUE.toString()),
						Config.BULK_API_SERIAL_MODE);
		return new UnionConfigGenerator(withoutBulkApi, withBulkApi);
	}

	public CsvProcessTest(String name, Map<String, String> config) {
		super(name, config);
	}

	public CsvProcessTest(String name) {
		super(name);
	}

	/**
	 * Tests the extract operation on Account. Verifies that an extract
	 * operation with a soql query is performed correctly.
	 * 
	 */

	public void testExtractAccountCsv() {
		int numRecords = 100;
		// insert accounts so there's something to query
		upsertSfdcAccounts(numRecords);

		try {
			String soql = "Select ID, NAME, TYPE, PHONE, ACCOUNTNUMBER__C, WEBSITE, ANNUALREVENUE, LASTMODIFIEDDATE, ORACLE_ID__C FROM ACCOUNT WHERE "
					+ ACCOUNT_WHERE_CLAUSE;
			Controller theController = runProcess(getExtractTestConfig(soql,
					true));

			// verify there were no errors during extract
			verifyNoError(theController);

			// verify successful output
			verifyAllSuccess(theController.getConfig().getString(
					Config.OUTPUT_SUCCESS), numRecords);
		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * Tests the insert operation on Account - Positive test.
	 */
	public void testInsertAccountCsv() {
		try {
			Controller theController = runProcess(getTestConfig(
					OperationInfo.insert.name(), false));

			// verify there were no errors during extract
			verifyNoError(theController);

			// verify there that all the records were successful (nothing lost)
			verifyAllSuccess(theController, null);

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * Tests update operation with input coming from a CSV file. Relies on the
	 * id's in the CSV on being in the database
	 */

	public void testUpdateAccountCsv() {
		try {
			Controller theController = runProcess(getUpdateTestConfig(false,
					null, Integer.MAX_VALUE));

			// verify there were no errors during operation
			verifyNoError(theController);

			// verify there that all the records were successful (nothing lost)
			verifyAllSuccess(theController, null);

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * Upsert the records from CSV file
	 */
	public void testUpsertAccountCsv() {
		try {
			Controller theController = runProcess(getUpdateTestConfig(true,
					DEFAULT_ACCOUNT_EXT_ID_FIELD, 50));

			// verify there were no errors during operation
			verifyNoError(theController);

			// verify that all the records were successful (nothing lost)
			verifyAllSuccess(theController, null);

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * Tests Upsert on foreign key for the records based on the CSV file
	 */
	public void testUpsertFkAccountCsv() {
		try {
			Controller theController = runProcess(getUpdateTestConfig(true,
					DEFAULT_ACCOUNT_EXT_ID_FIELD, 50));

			// verify there were no errors during operation
			verifyNoError(theController);

			// verify there that all the records were successful (nothing lost)
			verifyAllSuccess(theController, null);

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * Tests that Deleting the records based on a CSV file works
	 */
	public void testDeleteAccountCsv() throws Exception {
		// set batch process parameters
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, false);
		Map<String, String> argMap = getTestConfig(OperationInfo.delete.name(),
				deleteFileName, false);
		runDeleteAccountCsvTest(argMap);

	}

	/**
	 * Test output of last run files. 1. Output is enabled, directory is not set
	 * (use default) 2. Output is enabled, directory is set 3. Output is
	 * disabled
	 * 
	 * @hierarchy API.dataloader Csv Process Tests
	 * 
	 * @userstory Commenting existing data loader tests and uploading into QA
	 *            force
	 */
	public void testLastRunOutput() {
		// 1. Output is enabled (use default), directory is not set (use
		// default)
        testLastRunOutput(true, baseName + "_default", true, null);

		// 2. Output is enabled, directory is set
        testLastRunOutput(false, baseName + "_dirSet", true, System
				.getProperty("java.io.tmpdir"));

		// 3. Output is disabled
        testLastRunOutput(false, baseName + "_disabled", false, null);
	}

	/**
	 * Tests that SOQL queries with relationships work as expected
	 * 
	 * @throws ConnectionException
	 */
	public void testSoqlWithRelationships() throws ConnectionException {
		String contactId = "";
		String accountId = "";

		SObject account = new AccountObjectGetter().getObject(0, false);

		SaveResult[] srs = getBinding().create(new SObject[] { account });

		for (SaveResult sr : srs) {
			if (!sr.getSuccess()) {
				fail("Failed to create the Account SObject: " + sr.getErrors());
			} else {
				accountId = sr.getId();
			}
		}

		SObject contact = new ContactObjectGetter().getObject(0, false);

		// Relate the contact to the previously created account
		contact.setField("AccountId", accountId);

		SaveResult[] srs2 = getBinding().create(new SObject[] { contact });

		for (SaveResult sr : srs2) {
			if (!sr.getSuccess()) {
				fail("Failed to create the Contact SObject: " + sr.getErrors());
			} else { // Contact
				contactId = sr.getId();
			}
		}

		// TEST
		try {
			// set batch process parameters
			final String soql = "Select Id, Name, Account.Name From Contact Where Id = '"
					+ contactId + "'";
			final Map<String, String> argMap = getExtractTestConfig(soql, true);
			final String extractFileName = argMap.get(Config.DAO_NAME);
			runProcess(argMap);

			// verify there were no errors during extract
			CSVFileReader resultReader = new CSVFileReader(extractFileName);
			try {
				int numRows = resultReader.getTotalRows();
				assertEquals("Did not get one row in the results file: "
						+ extractFileName, 1, numRows);
				Map<String, Object> s = resultReader.readRow();

				assertEquals("Query returned incorrect Contact ID", contactId,
						s.get("CONTACT_ID"));
				assertEquals("Query returned incorrect Account Name", account
						.getField("Name"), s.get("ACCOUNT_NAME"));

			} catch (DataAccessObjectException e) {
				fail("Error accessing result output file: " + extractFileName);
			} finally {
				if (resultReader != null) {
					resultReader.close();
				}
			}

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		}
	}

	/**
	 * @param enableLastRunOutput
	 */
    private void testLastRunOutput(boolean useDefault, String baseProcessName,
			boolean enableOutput, String outputDir) {
		final String soql = "Select ID FROM ACCOUNT WHERE "
				+ ACCOUNT_WHERE_CLAUSE + " limit 1";
		Map<String, String> argMap = getExtractTestConfig(soql, true);
		argMap.remove(Config.MAPPING_FILE);

		// set last run output paramerers
		if (!useDefault) {
			argMap.put(Config.ENABLE_LAST_RUN_OUTPUT, String
					.valueOf(enableOutput));
			argMap.put(Config.LAST_RUN_OUTPUT_DIR, outputDir);
		}

		ProcessRunner runner;
		String lastRunFilePath = null;
		try {
			runner = ProcessRunner.getInstance(argMap);
			runner.setName(baseProcessName);
			runner.run();
			Controller theController = runner.getController(); // get controller
			// created for
			// this process

			Config config = theController.getConfig();
			lastRunFilePath = config.getLastRunFilename();
			File lastRunFile = new File(lastRunFilePath);
			String defaultFileName = baseProcessName + "_lastRun.properties";
			File expectedFile = useDefault ? new File(config
					.constructConfigFilePath(defaultFileName)) : new File(
					outputDir, defaultFileName);
			if (enableOutput) {
				assertTrue("Could not find last run file: " + lastRunFilePath,
						lastRunFile.exists());
				assertEquals("Did not get expected last run file.",
						expectedFile, lastRunFile);
			} else {
				assertFalse("Last run file should not exist: "
						+ lastRunFilePath, lastRunFile.exists());
			}

		} catch (ProcessInitializationException e) {
			fail("Error initializing the process, error: " + e.getMessage());
		} catch (RuntimeException e) {
			fail("Exception has been caught, error: " + e.getMessage());
		} finally {
			if (lastRunFilePath != null && new File(lastRunFilePath).exists()) {
				new File(lastRunFilePath).delete();
			}
		}

	}

	private Map<String, String> getUpdateTestConfig(boolean isUpsert,
			String extIdField, int maxInserts) {
		final String op = isUpsert ? OperationInfo.upsert.name()
				: OperationInfo.update.name();
		final String updateFileName = convertTemplateToInput(this.baseName
				+ "Template.csv", this.baseName + ".csv", "NAME", !isUpsert
				|| extIdField == null, maxInserts, false, false);
		final Map<String, String> argMap = getTestConfig(op, updateFileName,
				false);
		if (isUpsert && extIdField != null)
			argMap.put(Config.EXTERNAL_ID_FIELD, extIdField);
		return argMap;
	}

	private Map<String, String> getExtractTestConfig(String soql,
			boolean isWrite) {
		Map<String, String> argMap = getTestConfig(
				OperationInfo.extract.name(), isWrite);
		argMap.put(Config.EXTRACT_SOQL, soql);
		argMap.put(Config.DEBUG_MESSAGES, "false"); // Don't debug by default,
		// as it slows down the
		// processing
		argMap.put(Config.DEBUG_MESSAGES_FILE, new File(getTestStatusDir(),
				baseName + "SoapTrace.log").getAbsolutePath());
		// output success file
		argMap.put(Config.ENABLE_EXTRACT_STATUS_OUTPUT, "true");
		argMap.put(Config.EXTRACT_REQUEST_SIZE, "2000");
		return argMap;
	}

}
