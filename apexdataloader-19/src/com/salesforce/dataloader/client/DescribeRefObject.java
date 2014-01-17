/**
 * 
 */
package com.salesforce.dataloader.client;

import java.util.Map;

import com.sforce.soap.partner.Field;

/**
 * Container for reference object info. Right now, it's used for upsert references than need the set of external id
 * fields
 * 
 * @author awarshavsky
 * @since before
 */
public class DescribeRefObject {
    
    private String objectName;
    private Map<String, Field> fieldInfoMap;

    DescribeRefObject(String objectName, Map<String,Field> fieldInfoMap) {
        this.objectName = objectName;
        this.fieldInfoMap = fieldInfoMap;
    }

    public Map<String, Field> getFieldInfoMap() {
        return fieldInfoMap;
    }

    public String getObjectName() {
        return objectName;
    }
}
