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

import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PushyConsoleControllerTest {

    private Stage stage;

    private PushyConsoleController pushyConsoleController;
    private ComposeNotificationController composeNotificationController;

    private static final String SIGNING_KEY_FILENAME = "APNsAuthKey_KEYIDKEYID.p8";

    @Rule
    public JavaFXThreadRule javaFXThreadRule = new JavaFXThreadRule();

    @BeforeClass
    public static void setUpBeforeClass() throws InvocationTargetException, InterruptedException {
        // We need to make sure JavaFX is initialized before doing anything else.
        SwingUtilities.invokeAndWait(JFXPanel::new);
    }

    @Before
    public void setUp() throws Exception {
        stage = new Stage();

        final ResourceBundle resourceBundle = PushyConsoleApplication.RESOURCE_BUNDLE;

        final FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"), resourceBundle);

        final Parent root = loader.load();
        pushyConsoleController = loader.getController();

        composeNotificationController = pushyConsoleController.composeNotificationController;
        composeNotificationController.setSaveComboBoxValues(false);

        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    @After
    public void tearDown() {
        stage.close();
    }

    @Test
    public void testSendWithoutRequiredFields() {
        assertFalse("Required fields should not be highlighted before attempting to send a notification.",
                composeNotificationController.isRequiredFieldGroupHighlighted());

        pushyConsoleController.handleSendNotificationButtonAction(null);

        assertTrue("Required fields should be highlighted after attempting to send a notification with missing information.",
                composeNotificationController.isRequiredFieldGroupHighlighted());
    }

    @Test
    public void testSendWithRequiredFields() throws Exception {
        assertFalse("Required fields should not be highlighted before attempting to send a notification.",
                composeNotificationController.isRequiredFieldGroupHighlighted());

        composeNotificationController.handleSigningKeyFileSelection(FileUtils.toFile(getClass().getResource(SIGNING_KEY_FILENAME)));
        composeNotificationController.keyIdComboBox.setValue("KEYID");
        composeNotificationController.teamIdComboBox.setValue("TEAMID");

        composeNotificationController.topicComboBox.setValue("com.example.topic");
        composeNotificationController.deviceTokenComboBox.setValue("EXAMPLETOKEN");
        composeNotificationController.payloadTextArea.setText("{}");

        pushyConsoleController.handleSendNotificationButtonAction(null);

        assertFalse("Required fields should not be highlighted after sending a notification without missing information.",
                composeNotificationController.isRequiredFieldGroupHighlighted());
    }

    @Test
    public void testHandlePushNotificationResponse() {
        assertTrue("Notification table should be empty before receiving push notification response.",
                pushyConsoleController.notificationResultTableView.getItems().isEmpty());

        pushyConsoleController.handlePushNotificationResponse(new PushNotificationResponse<ApnsPushNotification>() {

            @Override
            public ApnsPushNotification getPushNotification() {
                return new SimpleApnsPushNotification("Token", "Topic", "{ payload: \"example\" }");
            }

            @Override
            public boolean isAccepted() {
                return true;
            }

            @Override
            public UUID getApnsId() {
                return UUID.randomUUID();
            }

            @Override
            public String getRejectionReason() {
                return null;
            }

            @Override
            public Date getTokenInvalidationTimestamp() {
                return null;
            }
        });

        assertFalse("Notification table should not be empty after receiving push notification response.",
                pushyConsoleController.notificationResultTableView.getItems().isEmpty());
    }
}
