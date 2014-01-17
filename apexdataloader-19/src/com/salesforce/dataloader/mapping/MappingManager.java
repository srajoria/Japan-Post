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

package com.salesforce.dataloader.mapping;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.exception.MappingInitializationException;

/**
 * This class manages the map of csv columns to sforce fields
 *
 * @author lviripaeff
 * @since before
 */
public class MappingManager {

    private Map<String, MappingElement> mapping;

    private boolean sourceColumnsProvided = false;

    // logger
    static Logger logger = Logger.getLogger(MappingManager.class);

    public MappingManager(List<String> sourceColumnNames, String fileName) throws MappingInitializationException {
        initializeMapping(sourceColumnNames, loadProperties(fileName));
    }

    /**
     * Initialize mapping by matching source columns to target columns using case-incensitive comparison. This is useful
     * for cases when map is not provided, e.g., for extracting to a CSV file
     *
     * @param sourceColumnList
     * @param destColumnList
     */
    public void initializeMapping(List<String> sourceColumnList, List<String> destColumnList) {
        initializeMapping(sourceColumnList, destColumnList, false);
    }

    /**
     * Initialize mapping by matching source columns to target columns using case-incensitive comparison. This is useful
     * for cases when map is not provided, e.g., for extracting to a CSV file
     * If supportAggSoql is true, support both named and unnamed aggregate soql columns
     *
     * @param sourceColumnList
     * @param destColumnList
     * @param supportAggSoql
     */
    public void initializeMapping(List<String> sourceColumnList,
            List<String> destColumnList, boolean supportAggSoql) {
        // protect aginst the empty target
        if (destColumnList == null || destColumnList.isEmpty()) {
            initializeMapping(sourceColumnList);
        } else {
            this.mapping = new HashMap<String, MappingElement>();

            Map<String, String> destUpperToOrigMap = new HashMap<String, String>();
            // normalize the destination column list while still remembering the original column names
            for (String destCol : destColumnList) {
                destUpperToOrigMap.put(destCol.toUpperCase(), destCol);
                // Handle Joins automatically without mapping it out.
                if (destCol.indexOf('.') > 0) {
                    addMapping(destCol, destCol);
                }
            }

            for (String sourceCol : sourceColumnList) {
                if (destUpperToOrigMap.containsKey(sourceCol.toUpperCase())) {
                    addMapping(sourceCol, destUpperToOrigMap.get(sourceCol.toUpperCase()));
                }
            }

            if (supportAggSoql) {
                List<String> namedAggCols = new ArrayList<String>();
                List<String> unNamedAggCols = new ArrayList<String>();
                for (String destCol : destColumnList) {
                    if (!mapping.containsKey(destCol.toLowerCase())) {
                        if (destCol != null) {
                            destCol = destCol.trim();
                            if (destCol.length() > 0) {
                                // unnamed aggregate columns end with ')'
                                if (destCol.charAt(destCol.length() - 1) == ')') {
                                    unNamedAggCols.add(destCol);
                                } else {
                                    namedAggCols.add(destCol);
                                }
                            }
                        }
                    }
                }
                int unNamedAggColIndex = 0;
                for (String col : unNamedAggCols) {
                    // backend soql return expr0, expr1 etc for unnamed aggregate
                    // soql columns. remove the original unnamed aggregate soql
                    // column and replace it with expr
                    int index = destColumnList.indexOf(col);
                    destColumnList.remove(col);
                    String exprCol = "expr" + unNamedAggColIndex++;
                    destColumnList.add(index, exprCol);
                    addMapping(exprCol, exprCol);
                }
                for (String col : namedAggCols) {
                    String[] tokens = col.split("\\s");
                    // named aggregate soql columns must have atleast 2
                    // whitespace separated tokens
                    if (tokens.length > 1) {
                        // replace named aggregate soql columns with just the
                        // name part
                        int index = destColumnList.indexOf(col);
                        destColumnList.remove(col);
                        destColumnList.add(index, tokens[tokens.length - 1]);
                        addMapping(tokens[tokens.length - 1], tokens[tokens.length - 1]);
                    }
                }
            }
        }
    }

    private void initializeMapping(List<String> sourceColumnNames, Properties props) {
        initializeMapping(sourceColumnNames);
        addMappings(props);
    }

    /**
     * Initialize source columns
     *
     * @param sourceColumnNames
     */
    private void initializeMapping(List<String> sourceColumnNames) {
        this.mapping = new HashMap<String, MappingElement>();

        // instantiate the keys if provided
        if (sourceColumnNames == null || sourceColumnNames.isEmpty()) {
            this.sourceColumnsProvided = false;
        } else {
            this.sourceColumnsProvided = true;
            for (String key : sourceColumnNames) {
                addMapping(key, ""); //$NON-NLS-1$
            }
        }
    }

    /**
     * Clears the mapped values from the stored MappingElements
     */
    public void clearValues() {
        for (MappingElement elem : mapping.values()) {
            elem.setDestColumn(""); //$NON-NLS-1$
        }
    }

    private Properties loadProperties(String fileName) throws MappingInitializationException {

        Properties props = new Properties();
        if (fileName == null || fileName.length() < 1) return props;
        FileInputStream in = null;
        try {
            in = new FileInputStream(fileName);
            props.load(in);
        } catch (IOException e) {
            String errMsg = Messages.getFormattedString("MappingManager.errorLoad", e.getMessage());
            logger.error(errMsg, e); //$NON-NLS-1$
            throw new MappingInitializationException(errMsg);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {}
            }
        }
        return props;

    }

    public void addMappings(String fileName) throws MappingInitializationException {
        Properties props = loadProperties(fileName);
        addMappings(props);
    }

    public void addMappings(Properties props) {
        for (Object key : props.keySet()) {
            String sourceKey = (String)key;
            // add mapping either:
            // 1. if source columns were originally provided & the key exists
            // 2. source columns were not provided
            // 3. source is a constant
            if ((sourceColumnsProvided && containsMappingFor(sourceKey)) || !sourceColumnsProvided
                    || isConstant(sourceKey)) {
                addMapping(sourceKey, props.getProperty(sourceKey));
            }
        }
    }

    public boolean containsMappingFor(String key) {
        return mapping.containsKey(key.toLowerCase());
    }

    public String getMappingFor(String key) {
        MappingElement elem = mapping.get(key.toLowerCase());
        if (elem != null) {
            return elem.getDestColumn();
        } else {
            return null;
        }
    }

    public void setElements(MappingElement[] elements) {
        for (MappingElement me : elements) {
            mapping.put(me.getSourceColumn(), me);
        }
    }

    public void addMapping(String key, String value) {
        String keyLower = key.toLowerCase();
        MappingElement elem = mapping.get(keyLower);
        if (elem == null) {
            elem = new MappingElement(key, value);
            mapping.put(keyLower, elem);
        } else {
            elem.setDestColumn(value);
        }
    }

    public MappingElement[] getElements() {
        List<MappingElement> list = new ArrayList<MappingElement>(mapping.values());

        Collections.sort(list, new MappingElementComparator());
        MappingElement[] elem = list.toArray(new MappingElement[list.size()]);

        return elem;
    }

    public List<String> mapColumns(List<String> sourceColumns) {
        List<String> destColumns = new ArrayList<String>();
        for (String sourceColumn : sourceColumns) {
            String destColumn = getMappingFor(sourceColumn);
            if (destColumn != null) {
                destColumns.add(destColumn);
            }
        }
        return destColumns;
    }

    public HashMap<String, Object> mapData(Map<String, Object> sourceValues) {
        HashMap<String, Object> destValues = new HashMap<String, Object>();
        // map source to destination given the map
        for (MappingElement elem : mapping.values()) {
            String destCol = elem.getDestColumn();
            // if destination is not specified, continue to next mapping element
            if (destCol.length() == 0) {
                continue;
            }
            if (isConstant(elem.getSourceColumn())) {
                // map is mapping to a constant value
                destValues.put(destCol, getConstantValue(elem.getSourceColumn()));
            } else {
                // value is provided
                destValues.put(destCol, sourceValues.get(elem.getSourceColumn()));
            }
        }
        return destValues;
    }

    /**
     * @param value
     * @return Constant value
     */
    static private String getConstantValue(String value) {
        StringBuffer buf = new StringBuffer(value);
        buf.deleteCharAt(0);
        buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }

    /**
     * @param value
     * @return true if value is a constant and not a field name
     */
    static private boolean isConstant(String value) {
        if (value == null || value.length() < 2) return false;
        boolean isConstant = value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"';
        return isConstant;
    }

    /**
     * Saves the preferences to the file from which they were originally loaded.
     *
     * @exception java.io.IOException
     *                if there is a problem saving this store
     */
    public void save(String filename) throws IOException {
        if (filename == null) throw new IOException(Messages.getString("MappingManager.errorFileName")); //$NON-NLS-1$
        Properties props = new Properties();
        MappingElement elem;
        Iterator iter = mapping.values().iterator();
        while (iter.hasNext()) {
            elem = (MappingElement)iter.next();
            props.put(elem.getSourceColumn(), elem.getDestColumn());
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            save(out, "Mapping values", props); //$NON-NLS-1$
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * Saves this config to the given output stream. The given string is inserted as header information.
     *
     * @param out
     *            the output stream
     * @param header
     *            the header
     * @param properties
     *            the properties to save
     * @exception java.io.IOException
     *                if there is a problem saving this store
     */
    private void save(OutputStream out, String header, Properties properties) throws IOException {
        properties.store(out, header);
    }

    /**
     * @return true if any mappings exist
     */
    public boolean isMappingExists() {
        return !mapping.isEmpty();
    }
}
