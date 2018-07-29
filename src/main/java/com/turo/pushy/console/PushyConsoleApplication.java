/*
 * Copyright (c) 2018 Turo Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.turo.pushy.console;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * The main application class for the Pushy Console. The application manages controller lifecycles and provides access
 * to shared resources.
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 */
public class PushyConsoleApplication extends Application {

    private PushyConsoleController pushyConsoleController;

    // Based heavily upon https://softwarei18n.org/using-unicode-in-java-resource-bundles-6220776b6099
    static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("com/turo/pushy/console/pushy-console", new ResourceBundle.Control() {

        @Override
        public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            final String bundleName = toBundleName(baseName, locale);

            final ResourceBundle bundle;

            switch (format) {
                case "java.class": {
                    bundle = super.newBundle(baseName, locale, format, loader, reload);
                    break;
                }

                case "java.properties": {
                    if (bundleName.contains("://")) {
                        bundle = null;
                    } else {
                        final String resourceName = toResourceName(bundleName, "properties");

                        try (final InputStream resourceInputStream = loader.getResourceAsStream(resourceName)) {
                            if (resourceInputStream != null) {
                                try (final InputStreamReader inputStreamReader = new InputStreamReader(resourceInputStream, StandardCharsets.UTF_8)) {
                                    bundle = new PropertyResourceBundle(inputStreamReader);
                                }
                            } else {
                                bundle = null;
                            }
                        }
                    }

                    break;
                }

                default: {
                    throw new IllegalArgumentException("Unknown format: " + format);
                }
            }

            return bundle;
        }
    });

    /**
     * Launches the Pushy Console application.
     *
     * @param args a list of command-line arguments
     */
    public static void main(final String... args) {
        launch(args);
    }

    /**
     * Loads resources and starts the application.
     *
     * @param primaryStage the primary stage for the application
     *
     * @throws Exception in the event of any problem loading and launching the application
     */
    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"), RESOURCE_BUNDLE);
        final Parent root = fxmlLoader.load();
        pushyConsoleController = fxmlLoader.getController();

        primaryStage.setTitle(RESOURCE_BUNDLE.getString("pushy-console.title"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    /**
     * Shuts down the application, disposing of any long-lived resources.
     *
     * @throws Exception if the application could not be shut down cleanly for any reason
     */
    @Override
    public void stop() throws Exception {
        super.stop();

        if (pushyConsoleController != null) {
            pushyConsoleController.stop();
        }
    }
}
