package com.salesforce.dataloader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

public class ConfigTestSuite extends TestSuite {

    public static Test createTest(Class<? extends Test> cls, String name, Map<String, String> config) {
        Constructor<? extends Test> ctor;
        try {
            ctor = ConfigTestSuite.getTestCtor(cls);
        } catch (final NoSuchMethodException e) {
            return warning("Class " + cls.getName()
                    + " has no public constructor ConfigTestBase(String name, Map<String,String> config)");
        }
        try {
            return ctor.newInstance(name, config);
        } catch (final InstantiationException e) {
            return warning("Cannot instantiate test case: " + name + " ("
                    + ConfigTestSuite.exceptionToString(e) + ")");
        } catch (final InvocationTargetException e) {
            return warning("Exception in constructor: " + name + " ("
                    + ConfigTestSuite.exceptionToString(e.getTargetException()) + ")");
        } catch (final IllegalAccessException e) {
            return warning("Cannot access test case: " + name + " (" + ConfigTestSuite.exceptionToString(e)
                    + ")");
        }

    }

    /**
     * Converts the stack trace into a string
     */
    private static String exceptionToString(Throwable t) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        return stringWriter.toString();

    }

    public static Constructor<? extends Test> getTestCtor(Class<? extends Test> cls) throws NoSuchMethodException {
        return cls.getConstructor(String.class, Map.class);
    }

    public ConfigTestSuite(Class<? extends TestCase> testClass, Map<String, String> config) {
        super(getSuiteName(testClass, config));
        addTests(testClass, config);
    }

    public ConfigTestSuite(Class<? extends TestCase> testClass, List<Map<String, String>> configs) {
        super(getSuiteName(testClass, configs));
        addTests(testClass, configs);
    }

    private static String getSuiteName(Class<? extends TestCase> testClass, List<Map<String, String>> configs) {
        return configs.size() > 1 ? testClass.getName() : getSuiteName(testClass, configs.get(0));
    }

    private static String getSuiteName(Class<? extends TestCase> testClass, final Map<String, String> conf) {
        return testClass.getName() + (conf.isEmpty() ? "" : String.valueOf(conf));
    }

    public static ConfigTestSuite createSuite(Class<? extends TestCase> cls) {
        final ConfigGenerator configGenerator = ConfigTestSuite.getConfigGenerator(cls);
        if (configGenerator == null)
            throw new UnsupportedOperationException("Failed to get config generator from class " + cls.getName());
        return new ConfigTestSuite(cls, configGenerator);
    }

    public ConfigTestSuite(Class<? extends TestCase> cls, ConfigGenerator configGenerator) {
        this(cls, configGenerator.getConfigurations());
    }

    private static ConfigGenerator getConfigGenerator(Class<? extends TestCase> cls) {
        Method meth = null;
        try {
            meth = cls.getMethod("getConfigGenerator", (Class[])null);
            final int mods = meth.getModifiers();
            if (!(Modifier.isPublic(mods) && Modifier.isStatic(mods))) return null;
            if (!ConfigGenerator.class.isAssignableFrom(meth.getReturnType())) return null;
            return (ConfigGenerator)meth.invoke(null, (Object[])null);
        } catch (final NoSuchMethodException e) {} catch (final InvocationTargetException e) {} catch (final IllegalAccessException e) {}
        return null;
    }

    private void addTests(Class<? extends TestCase> testClass, List<Map<String, String>> configs) {
        if (configs.size() == 1)
            addTests(testClass, configs.get(0));
        else
            for (final Map<String, String> config : configs)
                addTest(new ConfigTestSuite(testClass, config));
    }

    private void addTests(Class<? extends TestCase> testClass, Map<String, String> config) {
        try {
            ConfigTestSuite.getTestCtor(testClass); // Avoid generating multiple error messages
        } catch (final NoSuchMethodException e) {
            addTest(warning("Class " + testClass.getName()
                    + " has no public constructor TestCase(String name, Map<String,String> config)"));
            return;
        }

        if (!Modifier.isPublic(testClass.getModifiers())) {
            addTest(warning("Class " + testClass.getName() + " is not public"));
            return;
        }

        Class superClass = testClass;
        final List<String> names = new ArrayList<String>();
        while (Test.class.isAssignableFrom(superClass)) {
            for (final Method meth : superClass.getDeclaredMethods())
                addTestMethod(meth, names, config, testClass);
            superClass = superClass.getSuperclass();
        }
        if (testCount() == 0) addTest(warning("No tests found in " + testClass.getName()));
    }

    private void addTestMethod(Method m, List<String> names, Map<String, String> config,
            Class<? extends TestCase> testClass) {
        final String name = m.getName();
        if (names.contains(name)) return;
        if (isTestMethod(m)) {
            if (!isPublicTestMethod(m)) {
                addTest(warning("Test method isn't public: " + m.getName()));
                return;
            }
            names.add(name);
            addTest(ConfigTestSuite.createTest(testClass, name, config));
        }
    }

    /**
     * Adapted from {@link TestSuite}.
     * 
     * @param m
     * @return
     */
    private boolean isPublicTestMethod(Method m) {
        return Modifier.isPublic(m.getModifiers());
    }

    /**
     * Adapted from {@link TestSuite}.
     * 
     * @param m
     * @return
     */
    private boolean isTestMethod(Method m) {
        final String name = m.getName();
        final Class[] parameters = m.getParameterTypes();
        final Class returnType = m.getReturnType();
        return parameters.length == 0 && name.startsWith("test") && returnType.equals(Void.TYPE);
    }

    /**
     * Returns a test which will fail and log a warning message.
     */
    public static Test warning(final String message) {
        return new TestCase("warning") {
            @Override
            protected void runTest() {
                fail(message);
            }
        };
    }
}
