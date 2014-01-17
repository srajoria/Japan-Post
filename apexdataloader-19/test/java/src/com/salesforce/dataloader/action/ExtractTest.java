/**
 *
 */
package com.salesforce.dataloader.action;

import java.util.Arrays;
import java.util.List;

import com.salesforce.dataloader.TestBase;
import com.salesforce.dataloader.action.ExtractAction;
import com.salesforce.dataloader.exception.ExtractException;

/**
 * Test for extract action
 *
 * @author awarshavsky
 * @since before
 */
public class ExtractTest extends TestBase {

    public ExtractTest(String name) {
        super(name);
    }

    public void testQueryParsePositive() {
        // test simple soql
        positiveQueryParse("select field1, field2, field3 from account",
                new String[] {"field1", "field2", "field3"});

        // test for bug#89726 -- use keywords in the soql
        positiveQueryParse("select fromField1, fromField2, fromField3 from account",
                new String[] {"fromField1", "fromField2", "fromField3"});
        positiveQueryParse("select selectField1, selectField2, selectField3 from account",
                new String[] {"selectField1", "selectField2", "selectField3"});
        positiveQueryParse("select    from  ,  select   from    select",
                new String[] {"from", "select"});
        positiveQueryParse("select from, select from select where select like '0' and from like '1'",
                new String[] {"from", "select"});
        positiveQueryParse("select max(name) mname from Account group by Industry",
                new String[] {"max(name) mname"});
        positiveQueryParse("select max(name) from Account group by Industry",
                new String[] {"max(name)"});
        positiveQueryParse("select max(name), industry, max(name) mname, industry from Account group by Industry",
                new String[] {"max(name)", "industry", "max(name) mname", "industry"});
}

    public void testQueryParseNegative() {
        // empty
        negativeQueryParse(null);
        negativeQueryParse("");

        // malformed queries
        negativeQueryParse(" from Account");
        negativeQueryParse("select field1, field2, field3 from ");
        negativeQueryParse("account field1, field2, field3 from select ");
    }

    /**
     * @param soqlString
     * @param expectedFields
     */
    private void positiveQueryParse(String soqlString, String[] expectedFieldArray) {
        try {
            List<String> expectedFields = Arrays.asList(expectedFieldArray);
            List<String> fields = ExtractAction.getColumnsFromSoql(soqlString);
            assertEquals("Expected list of fields: " + expectedFields.toString(), fields, expectedFields);
        } catch (ExtractException e) {
            fail("Error parsing query string: \'" + soqlString + "\', error: " + e.getMessage());
        }
    }

    /**
     * @param soqlString
     * @param expectedFields
     */
    private void negativeQueryParse(String soqlString) {
        try {
            List<String> fields = ExtractAction.getColumnsFromSoql(soqlString);
            fail("The parse should have failed with an error, instead of getting fields: " + fields.toString());
        } catch (ExtractException e) {
            assertNotNull("The parse error message should not be null", e.getMessage());
            assertTrue("The parse error message should not be empty", e.getMessage().length() > 0);
        }
    }

}
