package com.turo.pushy.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

class PushyConsoleResources {
    private static final String BASE_NAME = "com/turo/pushy/console/pushy-console";
    private static final Utf8ResourceBundleControl UTF_8_RESOURCE_BUNDLE_CONTROL = new Utf8ResourceBundleControl();

    // Based heavily upon https://softwarei18n.org/using-unicode-in-java-resource-bundles-6220776b6099.
    private static class Utf8ResourceBundleControl extends ResourceBundle.Control {
        public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            final String bundleName = toBundleName(baseName, locale);

            final ResourceBundle bundle;

            if ("java.class".equals(format)) {
                bundle = super.newBundle(baseName, locale, format, loader, reload);
            } else if ("java.properties".equals(format)) {
                if (bundleName.contains("://")) {
                    bundle = null;
                } else {
                    final String resourceName = toResourceName(bundleName, "properties");

                    try (final InputStream resourceInputStream = loader.getResourceAsStream(resourceName)) {
                        if (resourceInputStream != null) {
                            try (final InputStreamReader inputStreamReader = new InputStreamReader(resourceInputStream, StandardCharsets.UTF_8)){
                                bundle = new PropertyResourceBundle(inputStreamReader);
                            }
                        } else {
                            bundle = null;
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown format: " + format);
            }

            return bundle;
        }
    }

    public static ResourceBundle getResourceBundle() {
        return ResourceBundle.getBundle(BASE_NAME, UTF_8_RESOURCE_BUNDLE_CONTROL);
    }
}
