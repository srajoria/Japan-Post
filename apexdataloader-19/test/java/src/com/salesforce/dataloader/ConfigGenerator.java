package com.salesforce.dataloader;

import java.util.List;
import java.util.Map;

public interface ConfigGenerator {

    int getNumConfigurations();

    List<Map<String, String>> getConfigurations();

}
