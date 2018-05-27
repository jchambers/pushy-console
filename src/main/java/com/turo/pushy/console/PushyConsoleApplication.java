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

public class PushyConsoleApplication extends Application {

    private PushyConsoleController pushyConsoleController;

    public static final ResourceBundle RESOURCE_BUNDLE =
            ResourceBundle.getBundle("com/turo/pushy/console/pushy-console", new ResourceBundle.Control() {

        @Override
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
    });

    public static void main(final String... args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"), RESOURCE_BUNDLE);
        final Parent root = fxmlLoader.load();
        pushyConsoleController = fxmlLoader.getController();

        primaryStage.setTitle(RESOURCE_BUNDLE.getString("pushy-console.title"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        if (pushyConsoleController != null) {
            pushyConsoleController.stop();
        }
    }
}
