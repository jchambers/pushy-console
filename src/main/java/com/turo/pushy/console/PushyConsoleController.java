package com.turo.pushy.console;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.PushNotificationResponse;
import io.netty.util.concurrent.Future;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PushyConsoleController {

    @FXML private ResourceBundle resources;

    @FXML private ComposeNotificationController composeNotificationController;

    @FXML private TableView<PushNotificationResponse<ApnsPushNotification>> notificationResultTableView;

    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultTopicColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultTokenColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultPayloadColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultPriorityColumn;

    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultStatusColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultDetailsColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultApnsIdColumn;

    public void initialize() {
        notificationResultTopicColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getTopic()));

        notificationResultTokenColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getToken()));

        notificationResultPayloadColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getPayload()
                        .replace('\n', ' ')
                        .replaceAll("\\s+", " ")));

        notificationResultPriorityColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(
                cellDataFeatures.getValue().getPushNotification().getPriority() == DeliveryPriority.IMMEDIATE ?
                        this.resources.getString("delivery-priority.immediate") :
                        this.resources.getString("delivery-priority.conserve-power")));

        notificationResultStatusColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(
                cellDataFeatures.getValue().isAccepted() ?
                        this.resources.getString("notification-result.status.accepted") :
                        this.resources.getString("notification-result.status.rejected")));

        notificationResultDetailsColumn.setCellValueFactory(cellDataFeatures -> {
            final PushNotificationResponse<ApnsPushNotification> pushNotificationResponse = cellDataFeatures.getValue();

            final String details;

            if (pushNotificationResponse.isAccepted()) {
                details = this.resources.getString("notification-result.details.accepted");
            } else {
                if (pushNotificationResponse.getTokenInvalidationTimestamp() == null) {
                    details = pushNotificationResponse.getRejectionReason();
                } else {
                    details = new MessageFormat(this.resources.getString("notification-result.details.expiration")).format(
                            new Object[] {
                                    cellDataFeatures.getValue().getRejectionReason(),
                                    cellDataFeatures.getValue().getTokenInvalidationTimestamp() });
                }
            }

            return new ReadOnlyStringWrapper(details);
        });

        notificationResultApnsIdColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getApnsId().toString()));
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
                    this.notificationResultTableView.getItems().add(responseFuture.getNow());
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
