/*
 * Copyright (c) 2018 Turo Inc.
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
import com.turo.pushy.apns.auth.ApnsSigningKey;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.Assert.*;

public class ComposeNotificationControllerTest {

    private Stage stage;

    private ComposeNotificationController composeNotificationController;

    private static final String CERTIFICATE_FILENAME = "apns-client.p12";
    private static final String CERTIFICATE_PASSWORD = "pushy-test";
    private static final Set<String> CERTIFICATE_TOPICS = new HashSet<>(Arrays.asList(
            "com.relayrides.pushy",
            "com.relayrides.pushy.voip",
            "com.relayrides.pushy.complication"));

    private static final String STANDARD_NAME_SIGNING_KEY_FILENAME = "APNsAuthKey_KEYIDKEYID.p8";
    private static final String NONSTANDARD_NAME_SIGNING_KEY_FILENAME = "signing-key.p8";

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

        final FXMLLoader loader = new FXMLLoader(getClass().getResource("compose-notification.fxml"), resourceBundle);

        final Parent root = loader.load();
        composeNotificationController = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();
        stage.toFront();
    }

    @After
    public void tearDown() {
        stage.close();
    }

    @Test
    public void testSetSigningKeyWithStandardFilenameAsCredentials() throws Exception {
        testSetSigningKeyAsCredentials(FileUtils.toFile(getClass().getResource(STANDARD_NAME_SIGNING_KEY_FILENAME)));

        assertEquals("Key ID should be populated automatically when key file has a standard filename.",
                "KEYIDKEYID", composeNotificationController.keyIdComboBox.getValue());

        assertTrue("Team ID field should be focused after automatically setting key ID.",
                composeNotificationController.teamIdComboBox.isFocused());
    }

    @Test
    public void testSetSigningKeyWithNonstandardFilenameAsCredentials() throws Exception {
        testSetSigningKeyAsCredentials(FileUtils.toFile(getClass().getResource(NONSTANDARD_NAME_SIGNING_KEY_FILENAME)));

        assertTrue("Key ID should be blank when key file has a non-standard filename.",
                StringUtils.isBlank(composeNotificationController.keyIdComboBox.getValue()));

        assertTrue("Key ID field should be focused if key ID has not been set automatically.",
                composeNotificationController.keyIdComboBox.isFocused());
    }

    private void testSetSigningKeyAsCredentials(final File signingKeyFile) throws Exception {
        assertTrue("Topic field should be editable before a credentials file has been selected.",
                composeNotificationController.topicComboBox.isEditable());

        assertTrue("Key ID field should be disabled until a signing key has been selected as a credential file.",
                composeNotificationController.keyIdComboBox.isDisabled());

        assertTrue("Key ID field should be blank before a credentials file has been selected.",
                StringUtils.isBlank(composeNotificationController.keyIdComboBox.getValue()));

        assertTrue("Team ID field should be disabled until a signing key has been selected as a credential file.",
                composeNotificationController.teamIdComboBox.isDisabled());

        assertTrue("Team ID field should be blank before a credentials file has been selected.",
                StringUtils.isBlank(composeNotificationController.teamIdComboBox.getValue()));

        composeNotificationController.handleSigningKeyFileSelection(signingKeyFile);

        assertEquals("Credential text field should be populated with file path after selecting a credential file.",
                signingKeyFile.getAbsolutePath(), composeNotificationController.apnsCredentialFileTextField.getText());

        assertTrue("Topic field should be editable after a signing key has been selected as a credential file.",
                composeNotificationController.topicComboBox.isEditable());

        assertFalse("Key ID field should be enabled after a signing key has been selected as a credential file.",
                composeNotificationController.keyIdComboBox.isDisabled());

        assertFalse("Team ID field should be enabled after a signing key has been selected as a credential file.",
                composeNotificationController.teamIdComboBox.isDisabled());
    }
    
    @Test
    public void testSetCertificateAsCredentials() throws Exception {
        assertTrue("Topic field should be editable before a credentials file has been selected.",
                composeNotificationController.topicComboBox.isEditable());

        assertTrue("Key ID field should be disabled if no credentials file has been selected.",
                composeNotificationController.keyIdComboBox.isDisabled());

        assertTrue("Key ID field should be blank before a credentials file has been selected.",
                StringUtils.isBlank(composeNotificationController.keyIdComboBox.getValue()));

        assertTrue("Team ID field should be disabled if no credentials file has been selected.",
                composeNotificationController.teamIdComboBox.isDisabled());

        assertTrue("Team ID field should be blank before a credentials file has been selected.",
                StringUtils.isBlank(composeNotificationController.teamIdComboBox.getValue()));

        final File certificateFile = FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME));

        composeNotificationController.handleCertificateFileAndPasswordSelection(certificateFile, CERTIFICATE_PASSWORD);

        assertEquals("Credential text field should be populated with file path after selecting a credential file.",
                certificateFile.getAbsolutePath(), composeNotificationController.apnsCredentialFileTextField.getText());

        assertFalse("Topic field should not be editable after a certificate key has been selected as a credential file.",
                composeNotificationController.topicComboBox.isEditable());

        assertEquals("Topic combo box should have all topics from chosen certificate as options.",
                CERTIFICATE_TOPICS, new HashSet<>(composeNotificationController.topicComboBox.getItems()));

        assertTrue("Key ID field should be disabled after a certificate has been selected as a credential file.",
                composeNotificationController.keyIdComboBox.isDisabled());

        assertTrue("Team ID field should be disabled after a certificate has been selected as a credential file.",
                composeNotificationController.teamIdComboBox.isDisabled());
    }

    @Test
    public void testGetCredentialsWithSigningKey() throws Exception {
        final String keyId = "KEYID";
        final String teamId = "TEAMID";

        assertFalse("APNs credentials should not be present before selecting a credentials file.",
                composeNotificationController.getApnsCredentials().isPresent());

        composeNotificationController.handleSigningKeyFileSelection(
                FileUtils.toFile(getClass().getResource(NONSTANDARD_NAME_SIGNING_KEY_FILENAME)));

        assertFalse("APNs credentials should not be present after selecting a signing key file, but before providing a key and tean ID.",
                composeNotificationController.getApnsCredentials().isPresent());

        composeNotificationController.keyIdComboBox.setValue(keyId);
        composeNotificationController.teamIdComboBox.setValue(null);

        assertFalse("APNs credentials should not be present after selecting a signing key file, but before providing a key and tean ID.",
                composeNotificationController.getApnsCredentials().isPresent());

        composeNotificationController.keyIdComboBox.setValue(null);
        composeNotificationController.teamIdComboBox.setValue(teamId);

        assertFalse("APNs credentials should not be present after selecting a signing key file, but before providing a key and tean ID.",
                composeNotificationController.getApnsCredentials().isPresent());

        composeNotificationController.keyIdComboBox.setValue(keyId);
        composeNotificationController.teamIdComboBox.setValue(teamId);

        assertTrue("APNs credentials should be present after providing a signing key file and a key ID and team ID.",
                composeNotificationController.getApnsCredentials().isPresent());

        final ApnsCredentials apnsCredentials = composeNotificationController.getApnsCredentials().get();

        assertTrue("Returned APNs credentials should contain a signing key.",
                apnsCredentials.getSigningKey().isPresent());

        final ApnsSigningKey signingKey = apnsCredentials.getSigningKey().get();

        assertEquals(keyId, signingKey.getKeyId());
        assertEquals(teamId, signingKey.getTeamId());
    }

    @Test
    public void testGetCredentialsWithCertificate() throws Exception {
        assertFalse("APNs credentials should not be present before selecting a credentials file.",
                composeNotificationController.getApnsCredentials().isPresent());

        composeNotificationController.handleCertificateFileAndPasswordSelection(
                FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME)), CERTIFICATE_PASSWORD);

        assertTrue("APNs credentials should be present after selecting a certificate file and password.",
                composeNotificationController.getApnsCredentials().isPresent());

        final ApnsCredentials apnsCredentials = composeNotificationController.getApnsCredentials().get();

        assertTrue("Returned APNs credentials should contain a certificate.",
                apnsCredentials.getCertificateAndPrivateKey().isPresent());
    }

    @Test
    public void testGetPushNotification() {
        assertFalse("Push notification should not be present until required fields are populated.",
                composeNotificationController.getPushNotification().isPresent());

        final String topic = "com.example.topic";
        final String token = "<3C3C1D11-9440927>";
        final String sanitizedToken = "3C3C1D119440927";
        final String payload = "{}";

        composeNotificationController.topicComboBox.setValue(topic);
        composeNotificationController.deviceTokenComboBox.setValue(token);
        composeNotificationController.payloadTextArea.setText(payload);

        assertTrue("Push notification should be present after required fields have been populated.",
                composeNotificationController.getPushNotification().isPresent());

        final ApnsPushNotification pushNotification = composeNotificationController.getPushNotification().get();

        assertEquals(topic, pushNotification.getTopic());
        assertEquals(sanitizedToken, pushNotification.getToken());
        assertEquals(payload, pushNotification.getPayload());
    }
}
