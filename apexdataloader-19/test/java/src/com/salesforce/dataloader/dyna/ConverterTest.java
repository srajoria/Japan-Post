package com.salesforce.dataloader.dyna;

import static com.salesforce.dataloader.dyna.DateConverter.GMT;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.beanutils.ConversionException;

import com.salesforce.dataloader.TestBase;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

/**
 * Tests the various dynabean type converters
 * 
 * @author lviripaeff
 */

public class ConverterTest extends TestBase {
    
    public ConverterTest(String name) {
        super(name);
    }

    public void testBooleanConverter() {
        BooleanConverter converter = new BooleanConverter();
        Boolean result;

        // test null and empty string, should return null
        result = (Boolean)converter.convert(null, null);
        assertNull(result);
        result = (Boolean)converter.convert(null, "");
        assertNull(result);

        // if we pass in a boolean, we should get the same one back
        result = (Boolean)converter.convert(null, Boolean.TRUE);
        assertEquals(Boolean.TRUE, result);

        result = (Boolean)converter.convert(null, Boolean.FALSE);
        assertEquals(Boolean.FALSE, result);

        // //////////////////////////
        // test the valid true values
        // //////////////////////////.
        result = (Boolean)converter.convert(null, "yes");
        assertEquals(Boolean.TRUE, result);

        result = (Boolean)converter.convert(null, "y");
        assertEquals(Boolean.TRUE, result);

        result = (Boolean)converter.convert(null, "true");
        assertEquals(Boolean.TRUE, result);

        result = (Boolean)converter.convert(null, "on");
        assertEquals(Boolean.TRUE, result);

        result = (Boolean)converter.convert(null, "1");
        assertEquals(Boolean.TRUE, result);

        // ///////////////////////////
        // Test the valid false values
        // ///////////////////////////
        result = (Boolean)converter.convert(null, "no");
        assertEquals(Boolean.FALSE, result);

        result = (Boolean)converter.convert(null, "n");
        assertEquals(Boolean.FALSE, result);

        result = (Boolean)converter.convert(null, "false");
        assertEquals(Boolean.FALSE, result);

        result = (Boolean)converter.convert(null, "off");
        assertEquals(Boolean.FALSE, result);

        result = (Boolean)converter.convert(null, "0");
        assertEquals(Boolean.FALSE, result);

        // For garbage, we throw a conversion Exception

        try {
            result = (Boolean)converter.convert(null, "qweorijo");
            fail();
        } catch (ConversionException e) {

        }
    }

    public void testDateConverter() {

        Calendar calDate;
        DateConverter converter = new DateConverter();

        // test null and empty string
        calDate = (Calendar)converter.convert(null, null);
        assertNull(calDate);

        calDate = (Calendar)converter.convert(null, "");
        assertNull(calDate);


        // if we pass in a calendar, should get the same Calendar back
        Calendar testCalDate = Calendar.getInstance();
        calDate = (Calendar)converter.convert(null, testCalDate);
        assertEquals(testCalDate, calDate);
        
        // if we pass in a date, we should get a Date back
        Date testDate = new Date();
        calDate = (Calendar)converter.convert(null, testDate);
        assertEquals(testDate, calDate.getTime());

        // use this as the expected calendar instance
        Calendar expCalDate = Calendar.getInstance(GMT);
        
        // test the valid date formats
        // NOTE: Month value is zero based, therefore subtract 1
        expCalDate.clear();
        expCalDate.set(2001, 11 - 1, 11, 10, 11, 40);
        _testValidDate("2001-11-11T10:10:100Z", expCalDate, false);
        // same date but with time zone
        _testValidDate("2001-11-11T02:10:100Z-0800", expCalDate, false);


        expCalDate.clear();
        expCalDate.set(2004, 3 - 1, 29, 11, 30, 23);
        _testValidDate("2004-03-29 11:30:23", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(1999, 12 - 1, 24, 11, 11, 11);
        expCalDate.set(Calendar.MILLISECOND, 111);
        _testValidDate("1999-12-24T11:11:11.111z", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(1977, 12 - 1, 24, 07, 36, 44);
        _testValidDate("19771224T07:36:44", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(1984, 04 - 1, 12, 6, 34, 22);
        _testValidDate("1984-04-12T06:34:22", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(1999, 9 - 1, 11);
        _testValidDate("1999-09-11", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(2009, 7 - 1, 16, 12, 14, 45);
        _testValidDate("07/16/2009 12:14:45", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(2007, 8 - 1, 23);
        _testValidDate("08/23/2007", expCalDate, false);

        // the following takes year as 0003, as in format yyyy-MM-dd, instead of MM-dd-yyyy
        // expCalDate.clear();
        // expCalDate.set(1981, 3 - 1, 30);
        // _testValidDate("3-30-1981", expCalDate, false);

        expCalDate.clear();
        expCalDate.set(2002, 2 - 1, 16);
        _testValidDate("2/16/2002", expCalDate, false);
        // the following literally gives year "02", not 2002.
        // _testValidDate("2/16/02", expCalDate, false);

        //
        // european date tests
        //
        expCalDate.clear();
        expCalDate.set(2009, 7 - 1, 16, 12, 14, 45);
        _testValidDate("16/7/2009 12:14:45", expCalDate, true);

        expCalDate.clear();
        expCalDate.set(2007, 8 - 1, 23);
        _testValidDate("23/08/2007", expCalDate, true);

        // the following takes year as 0003, as in format yyyy-MM-dd, instead of MM-dd-yyyy
        // expCalDate.clear();
        // expCalDate.set(1981, 3 - 1, 30);
        // _testValidDate("30-03-1981", expCalDate, true);

        expCalDate.clear();
        expCalDate.set(2002, 2 - 1, 16);
        _testValidDate("16/2/2002", expCalDate, true);
        // the following literally gives year "02", not 2002.
        // _testValidDate("16/2/02", expCalDate, true);

        try {
            _testValidDate("fofofod", null, false);
            fail();
        } catch (ConversionException e) {}

    }

    public void testDoubleConverter() {

    }

    public void testSObjectReferenceConverter() {
        SObjectReferenceConverter refConverter = new SObjectReferenceConverter();
        SObjectReference ref;

        try {
            getController().login();
        } catch (ConnectionException e) {
            fail("Error connecting to server: " + e.getMessage());
        }

        try {
            getController().setReferenceDescribes();
        } catch (ConnectionException e) {
            fail("Error getting object reference information:" + e.getMessage());
        }

        // null test
        ref = (SObjectReference)refConverter.convert(null, null);
        assertTrue(ref.isNull());

        // empty test
        ref = (SObjectReference)refConverter.convert(null, "");
        assertTrue(ref.isNull());

        // test getting SObjectReference back - string
        SObjectReference testRefStr = new SObjectReference("12345");
        ref = (SObjectReference)refConverter.convert(null, testRefStr.getReferenceExtIdValue());
        assertEquals(testRefStr, ref);

        // test getting SObjectReference back - number
        SObjectReference testRefNbr = new SObjectReference(12345);
        ref = (SObjectReference)refConverter.convert(null, testRefNbr.getReferenceExtIdValue());
        assertEquals(testRefNbr, ref);
        
        // test getting SObjectReference back - date
        SObjectReference testRefDate = new SObjectReference(Calendar.getInstance().getTime());
        ref = (SObjectReference)refConverter.convert(null, testRefDate.getReferenceExtIdValue());
        assertEquals(testRefDate, ref);
        
        // test validity of creating XML structure for foreign key ref
        testValidSObjectReference("12345", "Parent", true);
        testValidSObjectReference("12345", "Bogus", false);
    }

    /**
     * @param refValue
     * @param relationshipName
     * @param expectSuccess
     */
    private void testValidSObjectReference(String refValue, String relationshipName, boolean expectSuccess) {
        SObjectReference ref = new SObjectReference(refValue);
        SObject sObj = new SObject();
        String fkFieldName = DEFAULT_ACCOUNT_EXT_ID_FIELD;

        try {
            ref.addReferenceToSObject(getController(), sObj, ObjectField.formatAsString("Parent",
                    DEFAULT_ACCOUNT_EXT_ID_FIELD));

            SObject child = (SObject)sObj.getChild(relationshipName);
            boolean succeeded = child != null && child.getField(fkFieldName) != null && child.getField(fkFieldName)
                    .equals(refValue);
            if (expectSuccess && !succeeded || !expectSuccess && succeeded) {
                fail();
            }
        } catch (Exception e) {
            if (expectSuccess) {
                fail();
            }
        }
    }

    private void _testValidDate(String strDate, Calendar expCalDate, boolean useEuropean) {
        DateConverter converter = new DateConverter(useEuropean);
        Calendar calDate = (Calendar)converter.convert(null, strDate);
        assertNotNull(calDate);
        assertEquals(expCalDate.getTime(), calDate.getTime());
    }

}
