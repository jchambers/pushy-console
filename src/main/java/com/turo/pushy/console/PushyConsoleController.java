package com.turo.pushy.console;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import io.netty.util.concurrent.Future;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

public class PushyConsoleController implements Initializable {

    @FXML private ResourceBundle resources;

    @FXML private ComposeNotificationController composeNotificationController;

    @FXML private TableView<SendNotificationResult> notificationResultTableView;

    @FXML private TableColumn<SendNotificationResult, String> notificationResultTopicColumn;
    @FXML private TableColumn<SendNotificationResult, String> notificationResultTokenColumn;
    @FXML private TableColumn<SendNotificationResult, String> notificationResultPayloadColumn;
    @FXML private TableColumn<SendNotificationResult, String> notificationResultPriorityColumn;

    @FXML private TableColumn<SendNotificationResult, String> notificationResultStatusColumn;
    @FXML private TableColumn<SendNotificationResult, String> notificationResultDetailsColumn;
    @FXML private TableColumn<SendNotificationResult, String> notificationResultApnsIdColumn;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        notificationResultTopicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        notificationResultTokenColumn.setCellValueFactory(new PropertyValueFactory<>("token"));
        notificationResultPayloadColumn.setCellValueFactory(new PropertyValueFactory<>("payload"));
        notificationResultPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

        notificationResultStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        notificationResultDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        notificationResultApnsIdColumn.setCellValueFactory(new PropertyValueFactory<>("apnsId"));
    }

    @FXML
    protected void handleSendNotificationButtonAction(final ActionEvent event) {
        // TODO Make sure we have all the values we need

        this.composeNotificationController.saveCurrentFreeformValues();

        try {
            final ApnsClient apnsClient = this.composeNotificationController.buildClient();

            try {
                final Future<PushNotificationResponse<ApnsPushNotification>> responseFuture =
                        apnsClient.sendNotification(this.composeNotificationController.buildPushNotification()).await();

                if (responseFuture.isSuccess()) {
                    this.notificationResultTableView.getItems().add(new SendNotificationResult(responseFuture.getNow()));
                } else {
                    reportPushNotificationError(responseFuture.cause());
                }
            } finally {
                apnsClient.close();
            }
        } catch (final IOException | InvalidKeyException | NoSuchAlgorithmException | InterruptedException e) {
            reportPushNotificationError(e);
        }
    }

    private void reportPushNotificationError(final Throwable exception) {
        final Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setTitle(this.resources.getString("alert.notification-failed.title"));
        alert.setHeaderText(this.resources.getString("alert.notification-failed.header"));
        alert.setContentText(exception.getLocalizedMessage());

        final String stackTrace;
        {
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);

            stackTrace = stringWriter.toString();
        }

        final TextArea stackTraceTextArea = new TextArea(stackTrace);
        stackTraceTextArea.setEditable(false);
        stackTraceTextArea.setMaxWidth(Double.MAX_VALUE);
        stackTraceTextArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(stackTraceTextArea);

        alert.showAndWait();
    }
}
