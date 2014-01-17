package com.salesforce.dataloader;

import java.util.*;

public abstract class ConfigTestBase extends TestBase {

	public static ConfigGenerator DEFAULT_CONFIG_GEN = new ConfigGenerator() {

		public List<Map<String, String>> getConfigurations() {
			List<Map<String, String>> result = new ArrayList<Map<String, String>>();
			result.add(new HashMap<String, String>());
			return result;
		}

		public int getNumConfigurations() {
			return 1;
		}

	};

	public static class UnionConfigGenerator implements ConfigGenerator {
		private final ConfigGenerator[] gens;

		public UnionConfigGenerator(ConfigGenerator... gens) {
			this.gens = gens == null ? new ConfigGenerator[0] : gens;
		}

		public List<Map<String, String>> getConfigurations() {
			List<Map<String, String>> configs = new ArrayList<Map<String, String>>(
					getNumConfigurations());
			for (ConfigGenerator g : this.gens) {
				if (g != null)
					configs.addAll(g.getConfigurations());
			}
			return configs;
		}

		public int getNumConfigurations() {
			int n = 0;
			for (ConfigGenerator g : this.gens)
				if (g != null)
					n += g.getNumConfigurations();
			return n;
		}

	}

	public static class ConfigSettingGenerator implements ConfigGenerator {
		private final ConfigGenerator gen;
		private final String setting;
		private final String[] values;

		public static ConfigSettingGenerator getBooleanGenerator(
				ConfigGenerator gen, String setting) {
			return new ConfigSettingGenerator(gen, setting, Boolean.TRUE
					.toString(), Boolean.FALSE.toString());
		}

		public ConfigSettingGenerator(String setting, String... values) {
			this(null, setting, values);
		}

		public ConfigSettingGenerator(ConfigGenerator gen, String setting,
				String... values) {
			this.gen = gen == null ? DEFAULT_CONFIG_GEN : gen;
			this.setting = setting;
			this.values = values;
			assert this.values != null && this.values.length > 0;
		}

		public List<Map<String, String>> getConfigurations() {
			final List<Map<String, String>> result = this.gen
					.getConfigurations();
			final int startSize = result.size();
			assert startSize > 0;
			for (int idx = 0; idx < startSize; idx++) {
				Map<String, String> config = result.get(idx);
				config.put(this.setting, this.values[0]);
				for (int j = 1; j < this.values.length; j++) {
					config = new HashMap<String, String>(config);
					config.put(this.setting, this.values[j]);
					result.add(config);
				}
			}
			return result;
		}

		public int getNumConfigurations() {
			return this.gen.getNumConfigurations() * this.values.length;
		}
	}

	private final Map<String, String> testConfig;

	protected Map<String, String> getTestConfig() {
		assert this.testConfig != null;
		return new HashMap<String, String>(this.testConfig);
	}

	protected ConfigTestBase(String name, Map<String, String> testConfig) {
		super(name);
		if (testConfig == null)
			testConfig = new HashMap<String, String>();
		this.testConfig = testConfig;
	}

	protected ConfigTestBase(String name) {
		this(name, null);
	}

	@Override
	public void setUp() {
		super.setUp();
		try {
			getController().getConfig().loadParameterOverrides(getTestConfig());
		} catch (Exception e) {
			fail(e);
		}
	}

}
