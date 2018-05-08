package com.turo.pushy.console;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposeNotificationController {

    @FXML private ResourceBundle resources;

    @FXML private ComboBox<String> apnsServerComboBox;
    @FXML private ComboBox<Integer> apnsPortComboBox;
    @FXML private TextField apnsCredentialFileTextField;

    @FXML private Label keyIdLabel;
    @FXML private ComboBox<String> keyIdComboBox;
    @FXML private Label teamIdLabel;
    @FXML private ComboBox<String> teamIdComboBox;

    @FXML private ComboBox<String> topicComboBox;
    @FXML private ComboBox<String> deviceTokenComboBox;
    @FXML private ComboBox<String> collapseIdComboBox;
    @FXML private ComboBox<DeliveryPriority> deliveryPriorityComboBox;
    @FXML private TextArea payloadTextArea;

    private String certificatePassword;

    private final Preferences preferences;

    private String mostRecentServer;
    private int mostRecentPort;
    private DeliveryPriority mostRecentDeliveryPriority;
    private final List<String> recentKeyIds = new ArrayList<>();
    private final List<String> recentTeamIds = new ArrayList<>();
    private final List<String> recentTopics = new ArrayList<>();
    private final List<String> recentDeviceTokens = new ArrayList<>();
    private final List<String> recentCollapseIds = new ArrayList<>();

    private static final String MOST_RECENT_SERVER_KEY = "mostRecentServer";
    private static final String MOST_RECENT_PORT_KEY = "mostRecentPort";
    private static final String MOST_RECENT_DELIVERY_PRIORITY_KEY = "mostRecentDeliveryPriority";
    private static final String RECENT_KEY_IDS_KEY = "recentKeyIds";
    private static final String RECENT_TEAM_IDS_KEY = "recentTeamIds";
    private static final String RECENT_TOPICS_KEY = "recentTopics";
    private static final String RECENT_TOKENS_KEY = "recentTokens";
    private static final String RECENT_COLLAPSE_IDS_KEY = "recentCollapseIds";

    private static final String PREFERENCES_LIST_SEPARATOR = "\n";

    private static final Pattern APNS_SIGNING_KEY_WITH_ID_PATTERN =
            Pattern.compile("^APNsAuthKey_([A-Z0-9]{10}).p8$", Pattern.CASE_INSENSITIVE);

    private static final Pattern CERTIFICATE_TOPIC_PATTERN = Pattern.compile(".*UID=([^,]+).*");

    public ComposeNotificationController() {
        this.preferences = Preferences.userNodeForPackage(getClass());

        this.mostRecentServer = this.preferences.get(MOST_RECENT_SERVER_KEY, ApnsClientBuilder.PRODUCTION_APNS_HOST);
        this.mostRecentPort = this.preferences.getInt(MOST_RECENT_PORT_KEY, ApnsClientBuilder.DEFAULT_APNS_PORT);

        try {
            this.mostRecentDeliveryPriority = DeliveryPriority.valueOf(
                    this.preferences.get(MOST_RECENT_DELIVERY_PRIORITY_KEY, DeliveryPriority.IMMEDIATE.name()));
        } catch (final IllegalArgumentException e) {
            this.mostRecentDeliveryPriority = DeliveryPriority.IMMEDIATE;
        }
    }

    public void initialize() {
        this.apnsServerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.mostRecentServer = newValue;
            this.preferences.put(MOST_RECENT_SERVER_KEY, this.mostRecentServer);
        });

        this.apnsServerComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.PRODUCTION_APNS_HOST,
                ApnsClientBuilder.DEVELOPMENT_APNS_HOST));

        this.apnsServerComboBox.setValue(this.mostRecentServer);

        this.apnsPortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.mostRecentPort = newValue;
            this.preferences.putInt(MOST_RECENT_PORT_KEY, newValue);
        });

        this.apnsPortComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.DEFAULT_APNS_PORT,
                ApnsClientBuilder.ALTERNATE_APNS_PORT));

        this.apnsPortComboBox.setValue(this.mostRecentPort);

        this.deliveryPriorityComboBox.setValue(this.mostRecentDeliveryPriority);
        this.deliveryPriorityComboBox.setCellFactory(listView -> new DeliveryPriorityListCell());
        this.deliveryPriorityComboBox.setButtonCell(new DeliveryPriorityListCell());

        this.deliveryPriorityComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.mostRecentDeliveryPriority = newValue;
            this.preferences.put(MOST_RECENT_DELIVERY_PRIORITY_KEY, this.mostRecentDeliveryPriority.name());
        });

        this.deliveryPriorityComboBox.setItems(FXCollections.observableArrayList(
                DeliveryPriority.IMMEDIATE,
                DeliveryPriority.CONSERVE_POWER
        ));

        this.recentKeyIds.addAll(loadPreferencesList(RECENT_KEY_IDS_KEY));
        this.keyIdComboBox.setItems(FXCollections.observableArrayList(this.recentKeyIds));

        this.recentTeamIds.addAll(loadPreferencesList(RECENT_TEAM_IDS_KEY));
        this.teamIdComboBox.setItems(FXCollections.observableArrayList(this.recentTeamIds));

        this.recentTopics.addAll(loadPreferencesList(RECENT_TOPICS_KEY));
        this.topicComboBox.setItems(FXCollections.observableArrayList(this.recentTopics));

        this.recentDeviceTokens.addAll(loadPreferencesList(RECENT_TOKENS_KEY));
        this.deviceTokenComboBox.setItems(FXCollections.observableArrayList(this.recentDeviceTokens));

        this.recentCollapseIds.addAll(loadPreferencesList(RECENT_COLLAPSE_IDS_KEY));
        this.collapseIdComboBox.setItems(FXCollections.observableArrayList(this.recentCollapseIds));
    }

    @FXML
    protected void handleBrowseButtonAction(final ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PKCS#8 and PKCS#12 files", Arrays.asList("*.p8", "*.p12")),
                new FileChooser.ExtensionFilter("PKCS#12 files (certificates)", "*.p12"),
                new FileChooser.ExtensionFilter("PKCS#8 files (signing keys)", "*.p8"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        final File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try {
                ApnsSigningKey.loadFromPkcs8File(file, "temp", "temp");

                this.certificatePassword = null;
                this.apnsCredentialFileTextField.setText(file.getAbsolutePath());

                this.keyIdLabel.setDisable(false);
                this.keyIdComboBox.setDisable(false);
                this.teamIdLabel.setDisable(false);
                this.teamIdComboBox.setDisable(false);

                final String currentTopic = this.topicComboBox.getValue();

                this.topicComboBox.setEditable(true);
                this.topicComboBox.setItems(FXCollections.observableArrayList(this.recentTopics));
                this.topicComboBox.setValue(currentTopic);

                final Matcher matcher = APNS_SIGNING_KEY_WITH_ID_PATTERN.matcher(file.getName());

                if (matcher.matches()) {
                    if (keyIdComboBox.getValue() == null || keyIdComboBox.getValue().trim().isEmpty()) {
                        this.keyIdComboBox.setValue(matcher.group(1));
                        this.teamIdComboBox.requestFocus();
                    }
                } else {
                    this.keyIdComboBox.requestFocus();
                }
            } catch (final NoSuchAlgorithmException | IOException | InvalidKeyException e) {
                // Couldn't load the given file as a signing key. Try it as a P12 certificate instead.
                final PasswordInputDialog passwordInputDialog = new PasswordInputDialog(password -> {
                    try {
                        getFirstPrivateKeyEntry(file, password);
                        return true;
                    } catch (final KeyStoreException | IOException e1) {
                        return false;
                    }
                });

                final MessageFormat headerFormat = new MessageFormat(resources.getString("certificate-password-dialog.header"));

                passwordInputDialog.setTitle(resources.getString("certificate-password-dialog.title"));
                passwordInputDialog.setHeaderText(headerFormat.format(new String[] { file.getName() }));
                passwordInputDialog.setContentText(resources.getString("certificate-password-dialog.prompt"));

                final Optional<String> verifiedPassword = passwordInputDialog.showAndWait();

                verifiedPassword.ifPresent(password -> {
                    this.certificatePassword = password;
                    this.apnsCredentialFileTextField.setText(file.getAbsolutePath());

                    this.keyIdComboBox.setValue(null);
                    this.teamIdComboBox.setValue(null);

                    this.keyIdLabel.setDisable(true);
                    this.keyIdComboBox.setDisable(true);
                    this.teamIdLabel.setDisable(true);
                    this.teamIdComboBox.setDisable(true);

                    this.topicComboBox.setEditable(false);

                    final String currentTopic = this.topicComboBox.getValue();

                    final String baseTopicFromCertificate;

                    try {
                        final KeyStore.PrivateKeyEntry privateKeyEntry = getFirstPrivateKeyEntry(file, password);
                        final X509Certificate certificate = (X509Certificate) privateKeyEntry.getCertificate();

                        final Matcher topicMatcher =
                                CERTIFICATE_TOPIC_PATTERN.matcher(certificate.getSubjectX500Principal().getName());

                        if (topicMatcher.matches()) {
                            baseTopicFromCertificate = topicMatcher.group(1);
                        } else {
                            // TODO Show an alert instead
                            throw new RuntimeException(e);
                        }
                    } catch (final KeyStoreException | IOException e1) {
                        // This should never happen because we already loaded the certificate to check the password.
                        throw new RuntimeException(e);
                    }

                    this.topicComboBox.setItems(FXCollections.observableArrayList(
                            baseTopicFromCertificate,
                            baseTopicFromCertificate + ".voip",
                            baseTopicFromCertificate + ".complication"));

                    this.topicComboBox.setValue(this.topicComboBox.getItems().contains(currentTopic) ?
                            currentTopic : this.topicComboBox.getItems().get(0));
                });
            }
        }
    }

    private static KeyStore.PrivateKeyEntry getFirstPrivateKeyEntry(final File p12File, final String password) throws KeyStoreException, IOException {
        final char[] passwordCharacters = password.toCharArray();
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (final FileInputStream certificateInputStream = new FileInputStream(p12File)) {
            keyStore.load(certificateInputStream, passwordCharacters);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException(e);
        }

        final Enumeration<String> aliases = keyStore.aliases();
        final KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(passwordCharacters);

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();

            KeyStore.Entry entry;

            try {
                try {
                    entry = keyStore.getEntry(alias, passwordProtection);
                } catch (final UnsupportedOperationException e) {
                    entry = keyStore.getEntry(alias, null);
                }
            } catch (final UnrecoverableEntryException | NoSuchAlgorithmException e) {
                throw new KeyStoreException(e);
            }

            if (entry instanceof KeyStore.PrivateKeyEntry) {
                return (KeyStore.PrivateKeyEntry) entry;
            }
        }

        throw new KeyStoreException("Key store did not contain any private key entries.");
    }

    void saveCurrentFreeformValues() {
        if (this.keyIdComboBox.getValue() != null && !this.keyIdComboBox.getValue().trim().isEmpty()) {
            addToListWithFixedSize(this.keyIdComboBox.getValue(), this.recentKeyIds, 10);

            this.keyIdComboBox.setItems(FXCollections.observableArrayList(this.recentKeyIds));
            this.savePreferencesList(RECENT_KEY_IDS_KEY, this.recentKeyIds);
        }

        if (this.teamIdComboBox.getValue() != null && !this.teamIdComboBox.getValue().trim().isEmpty()) {
            addToListWithFixedSize(this.teamIdComboBox.getValue(), this.recentTeamIds, 10);

            this.teamIdComboBox.setItems(FXCollections.observableArrayList(this.recentTeamIds));
            this.savePreferencesList(RECENT_TEAM_IDS_KEY, this.recentTeamIds);
        }

        addToListWithFixedSize(this.topicComboBox.getValue(), this.recentTopics, 10);

        this.topicComboBox.setItems(FXCollections.observableArrayList(this.recentTopics));
        this.savePreferencesList(RECENT_TOPICS_KEY, this.recentTopics);

        addToListWithFixedSize(this.deviceTokenComboBox.getValue(), this.recentDeviceTokens, 10);

        this.deviceTokenComboBox.setItems(FXCollections.observableArrayList(this.recentDeviceTokens));
        this.savePreferencesList(RECENT_TOKENS_KEY, this.recentDeviceTokens);

        if (this.collapseIdComboBox.getValue() != null && !this.collapseIdComboBox.getValue().trim().isEmpty()) {
            addToListWithFixedSize(this.collapseIdComboBox.getValue(), this.recentCollapseIds, 10);

            this.collapseIdComboBox.setItems(FXCollections.observableArrayList(this.recentCollapseIds));
            this.savePreferencesList(RECENT_COLLAPSE_IDS_KEY, this.recentCollapseIds);
        }
    }

    private void savePreferencesList(final String key, final List<String> values) {
        this.preferences.put(key, String.join(PREFERENCES_LIST_SEPARATOR, values));
    }

    private List<String> loadPreferencesList(final String key) {
        return Arrays.asList(this.preferences.get(key, "").split(PREFERENCES_LIST_SEPARATOR));
    }

    private static void addToListWithFixedSize(final String string, final List<String> list, final int maxSize) {
        // Move the new element to the front of the list even if it's already in there at another index.
        list.remove(string);
        list.add(0, string);

        while (list.size() > maxSize) {
            list.remove(list.size() - 1);
        }
    }

    ApnsClient buildClient() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final ApnsClientBuilder builder = new ApnsClientBuilder();

        builder.setApnsServer(this.apnsServerComboBox.getValue(), this.apnsPortComboBox.getValue());

        if (this.certificatePassword != null) {
            builder.setClientCredentials(new File(this.apnsCredentialFileTextField.getText()), this.certificatePassword);
        } else {
            builder.setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(this.apnsCredentialFileTextField.getText()),
                    this.teamIdComboBox.getValue(), this.keyIdComboBox.getValue()));
        }

        return builder.build();
    }

    ApnsPushNotification buildPushNotification() {
        final String collapseId = collapseIdComboBox.getValue() == null || collapseIdComboBox.getValue().trim().isEmpty() ?
                null : collapseIdComboBox.getValue();

        return new SimpleApnsPushNotification(
                this.deviceTokenComboBox.getValue(),
                this.topicComboBox.getValue(),
                this.payloadTextArea.getText(),
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)),
                this.deliveryPriorityComboBox.getValue(),
                collapseId);
    }
}
