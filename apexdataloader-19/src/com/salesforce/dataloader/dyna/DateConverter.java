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

package com.salesforce.dataloader.dyna;

import java.text.*;
import java.util.*;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.log4j.Logger;

public final class DateConverter implements Converter {

    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    static Logger logger = Logger.getLogger(DateConverter.class);
    /**
     * The default value specified to our Constructor, if any.
     */
    private Object defaultValue = null;

    /**
     * Should we return the default value on conversion errors?
     */
    private boolean useDefault = true;
    private boolean useEuroDates = false;

    public DateConverter() {
        this.defaultValue = null;
        this.useDefault = false;
        this.useEuroDates = false;

    }

    public DateConverter(boolean useEuroDateFormat) {
        this.defaultValue = null;
        this.useDefault = false;
        this.useEuroDates = useEuroDateFormat;
    }

    public DateConverter(Object defaultValue, boolean useEuroDateFormat) {
        this.defaultValue = defaultValue;
        this.useDefault = true;
        this.useEuroDates = useEuroDateFormat;
    }

    public DateConverter(Object defaultValue) {

        this.defaultValue = defaultValue;
        this.useDefault = true;

    }

    private Calendar parseDate(String dateString, String pattern) {
        final DateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(GMT);
        return parseDate(dateString, df);
    }

    private Calendar parseDate(String dateString, DateFormat fmt) {
        final ParsePosition pos = new ParsePosition(0);
        final Date date = fmt.parse(dateString, pos);
        // we only want to use the date if parsing succeeded and used the entire string
        if (date != null && pos.getIndex() == dateString.length()) {
            Calendar cal = Calendar.getInstance(GMT);
            cal.setTime(date);
            return cal;
        }
        return null;
    }

    /**
     * Attempts to parse a date string using the given formatting patterns
     * 
     * @param dateString
     *            The date string to parse
     * @param patterns
     *            Patterns to try. These will be used in the constructor for SimpleDateFormat
     * @return A Calendar object representing the given date string
     */
    private Calendar tryParse(String dateString, String... patterns) {
        if (patterns == null) return null;
        for (String pattern : patterns) {
            Calendar cal = parseDate(dateString, pattern);
            if (cal != null) return cal;
        }
        return null;
    }


    public Object convert(Class type, Object value) {
        if (value == null) { return null; }

        Calendar cal = Calendar.getInstance(GMT);

        if (value instanceof Date) {
            cal.setTime((Date)value);
            return cal;
        }

        if (value instanceof Calendar) { return value; }
        
        final String dateString = value.toString().trim();
        if (dateString.length() == 0) return null;

        for (String basePattern : new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss", "yyyyMMdd'T'HH:mm:ss" }) {
            cal = tryParse(dateString, basePattern, basePattern + "'Z'", basePattern + "'Z'Z", basePattern + "'z'Z",
                    basePattern + "'z'", basePattern + "z");
            if (cal != null) return cal;
        }

        // FIXME -- BUG: this format is picked up as a mistake instead of MM-dd-yyyy or dd-MM-yyyy
        cal = parseDate(dateString, "yyyy-MM-dd");
        if (cal != null) return cal;

        if (useEuroDates) {
            cal = tryParse(dateString, "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy");

            // FIXME -- Warning: this never gets picked up because of yyyy-MM-dd
            /*
             * Calendar cal = parseDate("dd-MM-yyyy", dateString); if (cal != null) return cal;
             */
        } else {
            cal = tryParse(dateString, "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy");

            //FIXME -- Warning: this never gets picked up because of yyyy-MM-dd
            /*
             * Calendar cal = parseDate("MM-dd-yyyy", dateString); if (cal != null) return cal;
             */
        }

        if (cal != null) return cal;

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
        df.setLenient(true);
        df.setTimeZone(GMT);
        cal = parseDate(dateString, df);
        if (cal != null) return cal;

        df = DateFormat.getDateInstance(DateFormat.SHORT);
        df.setLenient(true);
        df.setTimeZone(GMT);
        cal = parseDate(dateString, df);
        if (cal != null) return cal;

        if (useDefault) {
            return defaultValue;
        } else {
            throw new ConversionException("Date Conversion FAILED");
        }

    }

}