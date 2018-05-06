package com.turo.pushy.console;

import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UUID;

public class PushyConsoleController implements Initializable {

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
        this.notificationResultTableView.getItems().add(new SendNotificationResult(new PushNotificationResponse() {
            @Override
            public ApnsPushNotification getPushNotification() {
                return new SimpleApnsPushNotification("Device token", "Topic", "{ A payload in JSON format }");
            }

            @Override
            public boolean isAccepted() {
                return false;
            }

            @Override
            public UUID getApnsId() {
                return UUID.randomUUID();
            }

            @Override
            public String getRejectionReason() {
                return "Didn't feel like it";
            }

            @Override
            public Date getTokenInvalidationTimestamp() {
                return new Date();
            }
        }));
    }
}
