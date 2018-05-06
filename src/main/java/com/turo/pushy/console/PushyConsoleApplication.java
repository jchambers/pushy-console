package com.turo.pushy.console;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PushyConsoleApplication extends Application {

    public static void main(final String... args) {
        launch(args);
    }

    public void start(final Stage primaryStage) throws Exception {
        final Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));

        // TODO Localize
        primaryStage.setTitle("Pushy Console");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
