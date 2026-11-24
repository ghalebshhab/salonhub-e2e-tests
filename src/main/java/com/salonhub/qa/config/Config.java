package com.salonhub.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads test settings. A value is found in this order:
 * <ol>
 *   <li>JVM system property, for example {@code -Dui.baseUrl=http://localhost:5173}</li>
 *   <li>Environment variable, the key in upper case with dots as underscores ({@code UI_BASEURL})</li>
 *   <li>{@code test-accounts.properties} (git-ignored, holds passwords)</li>
 *   <li>{@code config.properties} (committed defaults)</li>
 * </ol>
 */
public final class Config {

    private static final Properties DEFAULTS = load("config.properties", true);
    private static final Properties ACCOUNTS = load("test-accounts.properties", false);

    private Config() {
    }

    public static String get(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (isBlank(value)) {
            value = ACCOUNTS.getProperty(key);
        }
        if (isBlank(value)) {
            value = DEFAULTS.getProperty(key);
        }
        if (isBlank(value)) {
            throw new IllegalStateException("Missing test setting '" + key
                    + "'. Set it in src/test/resources/test-accounts.properties, as -D" + key
                    + " or as an environment variable.");
        }
        return value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String uiBaseUrl() {
        return stripSlash(get("ui.baseUrl"));
    }

    public static String apiBaseUrl() {
        return stripSlash(get("api.baseUrl"));
    }

    private static Properties load(String name, boolean required) {
        Properties properties = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException(name + " is not on the classpath");
                }
                return properties;
            }
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + name, e);
        }
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
