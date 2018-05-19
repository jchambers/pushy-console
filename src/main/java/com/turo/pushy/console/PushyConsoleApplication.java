package com.turo.pushy.console;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class PushyConsoleApplication extends Application {

    private PushyConsoleController pushyConsoleController;

    public static void main(final String... args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final ResourceBundle resourceBundle = PushyConsoleResources.getResourceBundle();

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"), resourceBundle);
        final Parent root = fxmlLoader.load();
        this.pushyConsoleController = fxmlLoader.getController();

        primaryStage.setTitle(resourceBundle.getString("pushy-console.title"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        if (this.pushyConsoleController != null) {
            this.pushyConsoleController.stop();
        }
    }
}
