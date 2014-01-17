/*
 * Copyright (c) 2005, salesforce.com, inc. All rights reserved. Redistribution and use in source and binary forms, with
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

import java.util.Map;

import junit.framework.TestSuite;

import com.salesforce.dataloader.ConfigTestSuite;
import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;

/**
 * Test for dataloader hard delete feature
 * 
 * @author vnarayanan
 * @since 166
 * @hierarchy API.dataloader Csv Process Tests
 * @userstory Commenting existing data loader tests and uploading into QA force
 */

public class CsvHardDeleteTest extends ProcessTestBase {

	public CsvHardDeleteTest(String name, Map<String, String> config) {
		super(name, config);
	}

	public CsvHardDeleteTest(String name) {
		super(name);
	}

	public static TestSuite suite() {
		return ConfigTestSuite.createSuite(CsvHardDeleteTest.class);
	}

	/**
	 * Hard Delete the records based on a CSV file. Verifies that there were no
	 * errors during this operation and success was returned. This operation
	 * permanently deletes records from the org.
	 */

	public void testHardDeleteAccountCsv() throws Exception {

		// do an insert of some account records to ensure there is some data to
		// hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, false);

		// set batch process parameters
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		Controller theController = runProcess(argMap);

		// verify there were no errors during operation
		verifyNoError(theController);

		// verify that all the records were successful
		verifyAllSuccess(theController, deleteFileName);

	}

	/**
	 * Hard Delete - negative test. Login with user who doesnt have bulk api
	 * hard delete user permission disabled and verify that hard delete
	 * operation cannot be performed
	 */
	public void testHardDeleteUserPermOff() throws Exception {
		// do an insert of some account records to ensure there is some data
		// to hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, false);
		// set batch process parameters
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		// load the user with profile perm off
		argMap.put(Config.USERNAME, "buser1@dataloader.org");

		try {
			Controller theController = runProcess(argMap);

			// Verify there were no successes to verify that the operation was
			// not performed
			verifyAllSuccess(theController.getConfig().getString(
					Config.OUTPUT_SUCCESS), 0);
			// verify there were no errors during operation
			verifyNoError(theController);

		} catch (RuntimeException e) {
			assertTrue(
					"You need the Bulk API Hard Delete user permission ... message not thrown to the user ",
					e.getCause() instanceof UnsupportedOperationException);

		}

	}

	/**
	 * Hard Delete - positive boundary test. Hard delete 1 record based on input
	 * from csv file.Verifies that there were no errors during this operation
	 * and success was returned. This operation permanently deletes records from
	 * the org.
	 */
	public void testHardDelete1AccountCsv() throws Exception {
		// do an insert of 1 account record to ensure there is some data to
		// hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, false);
		// set batch process parameters
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		Controller theController = runProcess(argMap);

		// verify there were no errors during operation
		verifyNoError(theController);

		// verify that all the records were successful (nothing lost)
		verifyAllSuccess(theController, deleteFileName);
	}

	/**
	 * Hard Delete - Negative test. An empty input csv file is used to verify
	 * that error message is thrown to user.
	 */
	public void testHardDeleteEmptyCsvFile() throws Exception {
		// create an empty csv file
		String deleteFileName = createEmptyInput(baseName + ".csv");
		// set batch process parameters
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);

		try {
			runProcess(argMap);

		} catch (RuntimeException e) {
			assertTrue(
					"CSV Error:Invalid format ... message not thrown to the user ",
					e.getCause() instanceof DataAccessObjectInitializationException);

		}
	}

	/**
	 * Hard Delete - Negative test. Uncheck Bull Api setting in data loader to
	 * verify that Hard Delete operation cannot be done.
	 */
    public void testHardDeleteBulkApiSetToFalse() throws Exception {
        // do an insert of some account records to ensure there is some data
        // to hard delete

        String deleteFileName = convertTemplateToInput(baseName + "Template.csv", baseName + ".csv", "", true,
                Integer.MAX_VALUE, false, false);
        // set batch process parameters
        Map<String, String> argMap = getHardDeleteTestConfig(false, deleteFileName, false, null);
        try {
            runProcess(argMap);
            fail("hard delete should not succeed if bulk api is turned off");
        } catch (Exception e) {
            final String msg = e.getMessage();
            final String expected = "java.lang.UnsupportedOperationException: Error instantiating operation hard_delete: could not instantiate class: null.";
            assertEquals("Wrong exception thrown when attempting to do hard delete with bulk api off : ", expected, msg);
        }

    }

	/**
	 * Hard Delete - Negative test. Input a csv file with invalid id to verify
	 * that the test fails.
	 */
	public void testHardDeleteInvalidInput() throws Exception {
		// do an insert of some account records to ensure there is some data
		// to hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", false, 0, true, false);
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		Controller theController = runProcess(argMap);

		// verify there were errors during operation
		verifyErrors(theController, "MALFORMED_ID:bad id");
	}

	/**
	 * Hard Delete - Negative test. Input a csv file with 1 invalid id and 2
	 * other valid ids to verify that the test fails for the invalid id and
	 * passes for the valid id.
	 */

	public void testHardDeleteInvalidIDFailsOtherValidIDPasses()
			throws Exception {
		// do an insert of some account records to ensure there is some data to
		// hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, true, false);
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		Controller theController = runProcess(argMap);

		String successFileName = theController.getConfig().getString(
				Config.OUTPUT_SUCCESS);

		// verify there were errors during operation
		verifyErrors(theController, "MALFORMED_ID:bad id");

		// verify that the 2 records were successful
		verifyAllSuccess(successFileName, 2);

		// verify that the value of ids in success file matches the actual input
		// id value
		verifyAllSuccessIds(successFileName, deleteFileName, 2, 1);

	}

	/**
	 * Hard Delete - Negative test. Hard delete should fail when other object's
	 * ID is used.
	 */
	public void testHardDeleteIDFromOtherObjectFails() throws Exception {
		// create the input file
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, false);
		// set batch process parameters
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, "Contact");

		Controller theController = runProcess(argMap);

		// verify there were errors during operation
		verifyErrors(theController,
				"INVALID_ID_FIELD:Invalid Id for entity type");
		// verify there were no successes
		verifyAllSuccess(theController.getConfig().getString(
				Config.OUTPUT_SUCCESS), 0);

	}

	/**
	 * Hard Delete - Negative test. Hard delete succeeds for same object ID and
	 * fails for other object ID in same csv.
	 */
	public void testHardDeleteSameObjectIDSucceedsOtherObjectIDFails()
			throws Exception {
		// do an insert of some account records to ensure there is some data to
		// hard delete
		String deleteFileName = convertTemplateToInput(baseName
				+ "Template.csv", baseName + ".csv", "", true,
				Integer.MAX_VALUE, false, true);
		Map<String, String> argMap = getHardDeleteTestConfig(true,
				deleteFileName, false, null);
		Controller theController = runProcess(argMap);

		// verify there were errors during operation
		verifyErrors(theController,
				"INVALID_ID_FIELD:Invalid Id for entity type");

		// verify the id value matches the input id value for the 1 successful
		// records
		verifyAllSuccessIds(theController.getConfig().getString(
				Config.OUTPUT_SUCCESS), deleteFileName, 1, 0);

	}

	private Map<String, String> getHardDeleteTestConfig(boolean useBulkapi,
			String deleteFileName, boolean isWrite, String entityName) {
		Map<String, String> argMap = getTestConfig(OperationInfo.hard_delete
				.name(), deleteFileName, false);
		argMap.put(Config.USE_BULK_API, new Boolean(useBulkapi).toString());
		if (entityName != null)
			argMap.put(Config.ENTITY, "Contact");
		return argMap;
	}

}
