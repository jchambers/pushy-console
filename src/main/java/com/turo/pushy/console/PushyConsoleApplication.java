package com.turo.pushy.console;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class PushyConsoleApplication extends Application {

    public static void main(final String... args) {
        launch(args);
    }

    public void start(final Stage primaryStage) throws Exception {
        final ResourceBundle resourceBundle = PushyConsoleResources.getResourceBundle();

        final Parent root = FXMLLoader.load(getClass().getResource("main.fxml"),
                resourceBundle);

        primaryStage.setTitle(resourceBundle.getString("pushy-console.title"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
