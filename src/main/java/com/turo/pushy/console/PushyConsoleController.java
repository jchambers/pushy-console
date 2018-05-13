package com.turo.pushy.console;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.PushNotificationResponse;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final ExecutorService sendNotificationExecutorService = Executors.newSingleThreadExecutor();

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
        final Scene scene = ((Node) event.getSource()).getScene();

        if (this.composeNotificationController.hasRequiredFields()) {
            scene.getStylesheets().remove(PushyConsoleResources.HIGHLIGHT_EMPTY_FIELDS_STYLESHEET);

            this.composeNotificationController.handleNotificationSent();

            final Task<PushNotificationResponse<ApnsPushNotification>> sendNotificationTask = new Task<PushNotificationResponse<ApnsPushNotification>>() {

                @Override
                protected PushNotificationResponse<ApnsPushNotification> call() throws Exception {
                    final ApnsClient apnsClient = PushyConsoleController.this.composeNotificationController.buildClient();

                    try {
                        return apnsClient.sendNotification(
                                PushyConsoleController.this.composeNotificationController.buildPushNotification()).get();
                    } finally {
                        apnsClient.close();
                    }
                }
            };

            sendNotificationTask.setOnSucceeded(workerStateEvent -> {
                this.notificationResultTableView.getItems().add(sendNotificationTask.getValue());
            });

            sendNotificationTask.setOnFailed(workerStateEvent -> {
                reportPushNotificationError(sendNotificationTask.getException());
            });

            this.sendNotificationExecutorService.execute(sendNotificationTask);
        } else {
            scene.getStylesheets().add(PushyConsoleResources.HIGHLIGHT_EMPTY_FIELDS_STYLESHEET);
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
