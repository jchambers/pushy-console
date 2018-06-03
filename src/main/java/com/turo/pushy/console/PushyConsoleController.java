/*
 * Copyright (c) 2018 Turo
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

import com.turo.pushy.apns.*;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A controller for the main pushy console window. The console controller delegates notification composition to a
 * {@link ComposeNotificationController} and manages the actual transmission of push notifications and reporting of
 * results.
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 */
public class PushyConsoleController {

    @FXML private ResourceBundle resources;

    @FXML ComposeNotificationController composeNotificationController;

    @FXML TableView<PushNotificationResponse<ApnsPushNotification>> notificationResultTableView;

    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultTopicColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultTokenColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultPayloadColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultCollapseIdColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultPriorityColumn;

    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultStatusColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultDetailsColumn;
    @FXML private TableColumn<PushNotificationResponse<ApnsPushNotification>, String> notificationResultApnsIdColumn;

    private final BooleanProperty readyToSendProperty = new SimpleBooleanProperty();

    private final ExecutorService sendNotificationExecutorService = Executors.newSingleThreadExecutor();

    /**
     * Initializes the controller and its various controls and bindings.
     */
    public void initialize() {
        notificationResultTableView.setPlaceholder(new Label(resources.getString("notification-result.placeholder")));

        notificationResultTopicColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getTopic()));

        notificationResultTokenColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getToken()));

        notificationResultPayloadColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getPayload()
                        .replace('\n', ' ')
                        .replaceAll("\\s+", " ")));

        notificationResultCollapseIdColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getPushNotification().getCollapseId()));

        notificationResultPriorityColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(
                cellDataFeatures.getValue().getPushNotification().getPriority() == DeliveryPriority.IMMEDIATE ?
                        resources.getString("delivery-priority.immediate") :
                        resources.getString("delivery-priority.conserve-power")));

        notificationResultStatusColumn.setCellValueFactory(cellDataFeatures -> new ReadOnlyStringWrapper(
                cellDataFeatures.getValue().isAccepted() ?
                        resources.getString("notification-result.status.accepted") :
                        resources.getString("notification-result.status.rejected")));

        notificationResultDetailsColumn.setCellValueFactory(cellDataFeatures -> {
            final PushNotificationResponse<ApnsPushNotification> pushNotificationResponse = cellDataFeatures.getValue();

            final String details;

            if (pushNotificationResponse.isAccepted()) {
                details = resources.getString("notification-result.details.accepted");
            } else {
                if (pushNotificationResponse.getTokenInvalidationTimestamp() == null) {
                    details = pushNotificationResponse.getRejectionReason();
                } else {
                    details = new MessageFormat(resources.getString("notification-result.details.expiration")).format(
                            new Object[] {
                                    cellDataFeatures.getValue().getRejectionReason(),
                                    cellDataFeatures.getValue().getTokenInvalidationTimestamp() });
                }
            }

            return new ReadOnlyStringWrapper(details);
        });

        notificationResultApnsIdColumn.setCellValueFactory(cellDataFeatures ->
                new ReadOnlyStringWrapper(cellDataFeatures.getValue().getApnsId().toString()));

        readyToSendProperty.bind(new BooleanBinding() {
            {
                super.bind(composeNotificationController.apnsCredentialsProperty(),
                        composeNotificationController.pushNotificationProperty());
            }

            @Override
            protected boolean computeValue() {
                return composeNotificationController.apnsCredentialsProperty().get() != null &&
                        composeNotificationController.pushNotificationProperty().get() != null;
            }
        });
    }

    @FXML
    void handleSendNotificationButtonAction(final ActionEvent event) {
        if (readyToSendProperty.get()) {
            composeNotificationController.handleNotificationSent();

            final Task<PushNotificationResponse<ApnsPushNotification>> sendNotificationTask = new Task<PushNotificationResponse<ApnsPushNotification>>() {

                @Override
                protected PushNotificationResponse<ApnsPushNotification> call() throws Exception {
                    final String server = composeNotificationController.apnsServerProperty().get();
                    final int port = composeNotificationController.apnsPortProperty().get();
                    final ApnsCredentials credentials = composeNotificationController.apnsCredentialsProperty().get();

                    final ApnsClientBuilder apnsClientBuilder = new ApnsClientBuilder();
                    apnsClientBuilder.setApnsServer(server, port);

                    credentials.getCertificateAndPrivateKey().ifPresent(certificateAndPrivateKey ->
                            apnsClientBuilder.setClientCredentials(certificateAndPrivateKey.getKey(), certificateAndPrivateKey.getValue(), null));

                    credentials.getSigningKey().ifPresent(apnsClientBuilder::setSigningKey);

                    final ApnsClient apnsClient = apnsClientBuilder.build();

                    try {
                        return apnsClient.sendNotification(composeNotificationController.pushNotificationProperty().get()).get();
                    } finally {
                        apnsClient.close();
                    }
                }
            };

            sendNotificationTask.setOnSucceeded(workerStateEvent ->
                    handlePushNotificationResponse(sendNotificationTask.getValue()));

            sendNotificationTask.setOnFailed(workerStateEvent ->
                    reportPushNotificationError(sendNotificationTask.getException()));

            sendNotificationExecutorService.execute(sendNotificationTask);
        } else {
            composeNotificationController.setRequiredFieldGroupHighlighted(true);
        }
    }

    void handlePushNotificationResponse(final PushNotificationResponse<ApnsPushNotification> pushNotificationPushNotificationResponse) {
        notificationResultTableView.getItems().add(pushNotificationPushNotificationResponse);
    }

    private void reportPushNotificationError(final Throwable exception) {
        final Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setTitle(resources.getString("alert.notification-failed.title"));
        alert.setHeaderText(resources.getString("alert.notification-failed.header"));
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

    void stop() {
        sendNotificationExecutorService.shutdown();
    }
}
