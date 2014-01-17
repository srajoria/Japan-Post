/**
 *
 */
package com.salesforce.dataloader.mapping;

import java.io.File;
import java.util.*;

import com.salesforce.dataloader.TestBase;
import com.salesforce.dataloader.exception.MappingInitializationException;
import com.salesforce.dataloader.mapping.MappingManager;

/**
 * Set of unit tests for the data mapper
 *
 * @author awarshavsky
 * @since before
 */
public class MappingTest extends TestBase {

    public MappingTest(String name) {
        super(name);
    }

    static final String[] SOURCE_NAMES = new String[] {"sourceOne", "sourceTwo", "sourceThree"};
    static final String[] SOURCE_NAMES_CASE = new String[] {"SoUrCeOne", "SOURCEtwo", "sourcethree"};
    static final String[] SOURCE_VALUES = new String[] {"valueOne", "valueTwo", "valueThree"};
    static final String[] DEST_NAMES = new String[] {"destinationOne", "destinationTwo", "destinationThree"};
    static final String DEST_CONSTANT_NAME = "destinationConstant";
    static final String CONSTANT_VALUE = "constantValue123";
    private Map<String,Object> sourceValueMap;

    @Override
    public void setUp() {
        super.setUp();

        sourceValueMap = new HashMap<String,Object>();
        // populate all the available values
        for(int i=0; i < SOURCE_NAMES.length; i++) {
            sourceValueMap.put(SOURCE_NAMES[i], SOURCE_VALUES[i]);
        }
    }

    public void testMapFile() {
        try {
            MappingManager mapper = new MappingManager(Arrays.asList(SOURCE_NAMES),
                    new File(getTestDataDir(), "basicMap.sdl").getAbsolutePath());

            verifyMapping(mapper, DEST_NAMES);
        } catch (MappingInitializationException e) {
            fail("Error initializing the mapper: " + e.getMessage());
        }
    }

    public void testMapFileNoSourceColumns() {
        try {
            // null column list
            MappingManager mapper = new MappingManager(null, new File(getTestDataDir(), "basicMap.sdl").getAbsolutePath());
            verifyMapping(mapper, DEST_NAMES);

            // empty column list
            mapper = new MappingManager(new ArrayList<String>(), new File(getTestDataDir(), "basicMap.sdl").getAbsolutePath());
            verifyMapping(mapper, DEST_NAMES);
        } catch (MappingInitializationException e) {
            fail("Error initializing the mapper: " + e.getMessage());
        }
    }

    public void testMapProperties() {
        try {
            // prepopulate properties map
            Properties mappings = new Properties();
            for(int i=0; i < SOURCE_NAMES.length; i++) {
                mappings.put(SOURCE_NAMES[i], DEST_NAMES[i]);
            }
            mappings.put("\"" + CONSTANT_VALUE + "\"", DEST_CONSTANT_NAME);

            MappingManager mapper = new MappingManager(Arrays.asList(SOURCE_NAMES),null);
            mapper.addMappings(mappings);
            verifyMapping(mapper, DEST_NAMES);
        } catch (MappingInitializationException e) {
            fail("Error initializing the mapper: " + e.getMessage());
        }
    }

    public void testMapAutoMatch() {

        try {
            MappingManager mapper = new MappingManager(null, null);
            mapper.initializeMapping(Arrays.asList(SOURCE_NAMES), Arrays.asList(SOURCE_NAMES_CASE));
            mapper.addMapping("\"" + CONSTANT_VALUE + "\"", DEST_CONSTANT_NAME);
            verifyMapping(mapper, SOURCE_NAMES_CASE);
        } catch (MappingInitializationException e) {
            fail("Error initializing the mapper: " + e.getMessage());
        }
    }

    /**
     * @param mapper
     * @param destNames names of the destination columns
     */
    private void verifyMapping(MappingManager mapper, String[] destNames) {
        Map<String,Object> destValueMap = mapper.mapData(this.sourceValueMap);
        for(int i=0; i < destNames.length; i++) {
            assertNotNull("Destination# " + i + "(" + destNames[i] + ") should have a mapped value", destValueMap.get(destNames[i]));
            assertEquals ("Destination# " + i + "(" + destNames[i] + ") should contain the expected value", SOURCE_VALUES[i], destValueMap.get(destNames[i]));
        }
        // verify constant mapped correctly
        assertEquals("Destination[" + DEST_CONSTANT_NAME + "] should contain constant", CONSTANT_VALUE, destValueMap.get(DEST_CONSTANT_NAME));
    }
}
