package com.turo.pushy.console;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A controller for the area of the area of the main window where users enter connection settings, APNs credentials,
 * and notification details.
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 */
public class ComposeNotificationController {

    @FXML private ResourceBundle resources;

    @FXML ComboBox<String> apnsServerComboBox;
    @FXML ComboBox<Integer> apnsPortComboBox;
    @FXML TextField apnsCredentialFileTextField;

    @FXML Label keyIdLabel;
    @FXML ComboBox<String> keyIdComboBox;
    @FXML Label teamIdLabel;
    @FXML ComboBox<String> teamIdComboBox;

    @FXML ComboBox<String> topicComboBox;
    @FXML ComboBox<String> deviceTokenComboBox;
    @FXML ComboBox<String> collapseIdComboBox;
    @FXML ComboBox<DeliveryPriority> deliveryPriorityComboBox;
    @FXML MenuButton recentPayloadsMenuButton;
    @FXML TextArea payloadTextArea;

    private final ReadOnlyStringWrapper apnsServerWrapper = new ReadOnlyStringWrapper();
    private final ReadOnlyIntegerWrapper apnsPortWrapper = new ReadOnlyIntegerWrapper();

    private final ObjectProperty<Pair<File, String>> credentialsFileAndPasswordProperty = new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<ApnsCredentials> apnsCredentialsWrapper = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyObjectWrapper<ApnsPushNotification> pushNotificationWrapper = new ReadOnlyObjectWrapper<>();

    private final ListProperty<String> recentTopicsProperty = new SimpleListProperty<>();
    private final ObservableList<String> recentPayloads = FXCollections.observableArrayList();

    private final BooleanProperty requiredFieldGroupHighlightedProperty = new SimpleBooleanProperty();

    private static final String MOST_RECENT_SERVER_KEY = "mostRecentServer";
    private static final String MOST_RECENT_PORT_KEY = "mostRecentPort";
    private static final String MOST_RECENT_DELIVERY_PRIORITY_KEY = "mostRecentDeliveryPriority";
    private static final String RECENT_KEY_IDS_KEY = "recentKeyIds";
    private static final String RECENT_TEAM_IDS_KEY = "recentTeamIds";
    private static final String RECENT_TOPICS_KEY = "recentTopics";
    private static final String RECENT_TOKENS_KEY = "recentTokens";
    private static final String RECENT_COLLAPSE_IDS_KEY = "recentCollapseIds";
    private static final String RECENT_PAYLOADS_KEY = "recentPayloads";

    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private static final int MAX_COMBO_BOX_ITEMS = 10;

    private static final Pattern APNS_SIGNING_KEY_WITH_ID_PATTERN =
            Pattern.compile("^APNsAuthKey_([A-Z0-9]{10}).p8$", Pattern.CASE_INSENSITIVE);

    private static final PseudoClass BLANK_PSEUDO_CLASS = PseudoClass.getPseudoClass("blank");

    private static final String HIGHLIGHT_EMPTY_FIELDS_STYLESHEET =
            ComposeNotificationController.class.getResource("highlight-blank-fields.css").toExternalForm();

    /**
     * Initializes the controller and its various controls and bindings.
     */
    public void initialize() {
        final Preferences preferences = Preferences.userNodeForPackage(getClass());

        apnsServerComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.PRODUCTION_APNS_HOST,
                ApnsClientBuilder.DEVELOPMENT_APNS_HOST));

        apnsServerComboBox.setValue(preferences.get(MOST_RECENT_SERVER_KEY, ApnsClientBuilder.PRODUCTION_APNS_HOST));

        apnsPortComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.DEFAULT_APNS_PORT,
                ApnsClientBuilder.ALTERNATE_APNS_PORT));

        apnsPortComboBox.setValue(preferences.getInt(MOST_RECENT_PORT_KEY, ApnsClientBuilder.DEFAULT_APNS_PORT));

        apnsCredentialFileTextField.textProperty().bind(new StringBinding() {
            {
                super.bind(credentialsFileAndPasswordProperty);
            }

            @Override
            protected String computeValue() {
                final Pair<File, String> credentialsFileAndPassword = credentialsFileAndPasswordProperty.get();
                return credentialsFileAndPassword != null ? credentialsFileAndPassword.getKey().getAbsolutePath() : null;
            }
        });

        deliveryPriorityComboBox.setCellFactory(listView -> new DeliveryPriorityListCell());
        deliveryPriorityComboBox.setButtonCell(new DeliveryPriorityListCell());

        deliveryPriorityComboBox.setItems(FXCollections.observableArrayList(
                DeliveryPriority.IMMEDIATE,
                DeliveryPriority.CONSERVE_POWER
        ));

        try {
            deliveryPriorityComboBox.setValue(DeliveryPriority.valueOf(preferences.get(MOST_RECENT_DELIVERY_PRIORITY_KEY, DeliveryPriority.IMMEDIATE.name())));
        } catch (final IllegalArgumentException e) {
            deliveryPriorityComboBox.setValue(DeliveryPriority.IMMEDIATE);
        }

        recentTopicsProperty.set(FXCollections.observableArrayList(loadPreferencesList(RECENT_TOPICS_KEY)));
        recentTopicsProperty.addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TOPICS_KEY, change.getList()));

        topicComboBox.itemsProperty().bind(recentTopicsProperty);

        credentialsFileAndPasswordProperty.addListener((observable, oldValue, newValue) -> {
            // If we have a password, we're dealing with a certificate
            if (newValue != null && newValue.getValue() != null) {

                try {
                    final List<String> topics = new ArrayList<>(CertificateUtil.extractApnsTopicsFromCertificate(newValue.getKey(), newValue.getValue()));
                    topics.sort(Comparator.naturalOrder());

                    // When working with certificates, we'll always have a fixed list of topics from the certificate and
                    // should not allow freeform editing.
                    topicComboBox.setEditable(false);

                    topicComboBox.itemsProperty().unbind();
                    topicComboBox.setItems(FXCollections.observableArrayList(topics));

                    if (!topicComboBox.getItems().contains(topicComboBox.getValue())) {
                        topicComboBox.setValue(topicComboBox.getItems().get(0));
                    }
                } catch (final KeyStoreException | IOException e) {
                    // This should never happen since we checked the certificate when it was first selected
                    throw new RuntimeException(e);
                }
            } else {
                topicComboBox.setEditable(true);
                topicComboBox.itemsProperty().bind(recentTopicsProperty);
            }
        });

        final BooleanBinding credentialsFileIsNotSigningKeyBinding = new BooleanBinding() {
            {
                super.bind(credentialsFileAndPasswordProperty);
            }

            @Override
            protected boolean computeValue() {
                final Pair<File, String> credentialsFileAndPassword = credentialsFileAndPasswordProperty.get();
                return credentialsFileAndPassword == null || credentialsFileAndPassword.getValue() != null;
            }
        };

        keyIdLabel.disableProperty().bind(keyIdComboBox.disabledProperty());

        keyIdComboBox.disableProperty().bind(credentialsFileIsNotSigningKeyBinding);
        keyIdComboBox.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                keyIdComboBox.setValue(null);
            }
        });

        keyIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_KEY_IDS_KEY)));
        keyIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_KEY_IDS_KEY, change.getList()));

        teamIdLabel.disableProperty().bind(teamIdComboBox.disabledProperty());

        teamIdComboBox.disableProperty().bind(credentialsFileIsNotSigningKeyBinding);
        teamIdComboBox.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                teamIdComboBox.setValue(null);
            }
        });

        teamIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_TEAM_IDS_KEY)));
        teamIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TEAM_IDS_KEY, change.getList()));

        deviceTokenComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_TOKENS_KEY)));
        deviceTokenComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TOKENS_KEY, change.getList()));

        collapseIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_COLLAPSE_IDS_KEY)));
        collapseIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_COLLAPSE_IDS_KEY, change.getList()));

        recentPayloads.addListener((ListChangeListener<String>) change -> {
            recentPayloadsMenuButton.getItems().clear();

            recentPayloadsMenuButton.getItems().addAll(change.getList().stream().map(payload -> {
                final String smooshedPayload = payload.replaceAll("\\s+", " ");
                final MenuItem menuItem = new MenuItem(smooshedPayload);

                menuItem.setOnAction(event -> payloadTextArea.setText(payloadTextArea.getText() + payload));

                return menuItem;
            }).collect(Collectors.toList()));

            recentPayloadsMenuButton.setDisable(change.getList().isEmpty());
        });

        recentPayloads.addAll(loadPreferencesList(RECENT_PAYLOADS_KEY));

        recentPayloads.addListener((ListChangeListener<String>) change -> {
            int end = recentPayloads.size();

            while (end > 0) {
                try {
                    preferences.put(RECENT_PAYLOADS_KEY, GSON.toJson(recentPayloads.subList(0, end)));
                    break;
                } catch (final IllegalArgumentException e) {
                    end -= 1;
                }
            }
        });

        addEmptyPseudoClassListener(keyIdComboBox, teamIdComboBox, topicComboBox, deviceTokenComboBox);
        addEmptyPseudoClassListener(apnsCredentialFileTextField, payloadTextArea);

        requiredFieldGroupHighlightedProperty.addListener((observable, oldValue, newValue) -> {
            // We can get the scene from any node; there's nothing special about the server combo box.
            final Scene scene = apnsServerComboBox.getScene();

            if (newValue) {
                scene.getStylesheets().add(HIGHLIGHT_EMPTY_FIELDS_STYLESHEET);
            } else {
                scene.getStylesheets().remove(HIGHLIGHT_EMPTY_FIELDS_STYLESHEET);
            }
        });

        apnsServerWrapper.bind(apnsServerComboBox.valueProperty());
        apnsPortWrapper.bind(apnsPortComboBox.valueProperty());

        apnsCredentialsWrapper.bind(new ObjectBinding<ApnsCredentials>() {
            {
                super.bind(credentialsFileAndPasswordProperty,
                        keyIdComboBox.valueProperty(),
                        teamIdComboBox.valueProperty());
            }

            @Override
            protected ApnsCredentials computeValue() {
                final ApnsCredentials credentials;

                final Pair<File, String> credentialsFileAndPassword = credentialsFileAndPasswordProperty.get();

                if (credentialsFileAndPassword != null) {
                    if (credentialsFileAndPassword.getValue() != null) {
                        try {
                            credentials = new ApnsCredentials(credentialsFileAndPassword.getKey(), credentialsFileAndPassword.getValue());
                        } catch (final IOException | KeyStoreException e) {
                            // This should never happen because we checked the certificate when it was first selected
                            throw new RuntimeException(e);
                        }
                    } else {
                        final String keyId = keyIdComboBox.getValue();
                        final String teamId = teamIdComboBox.getValue();

                        final boolean hasKeyId = StringUtils.isNotBlank(keyId);
                        final boolean hasTeamId = StringUtils.isNotBlank(teamId);

                        try {
                            credentials = (hasKeyId && hasTeamId) ? new ApnsCredentials(credentialsFileAndPassword.getKey(), keyId, teamId) : null;
                        } catch (final NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                            // This should never happen because we checked the signing key when it was first selected
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    credentials = null;
                }

                return credentials;
            }
        });

        pushNotificationWrapper.bind(new ObjectBinding<ApnsPushNotification>() {
            {
                super.bind(deviceTokenComboBox.valueProperty(),
                        topicComboBox.valueProperty(),
                        payloadTextArea.textProperty(),
                        deliveryPriorityComboBox.valueProperty(),
                        collapseIdComboBox.valueProperty());
            }
            @Override
            protected ApnsPushNotification computeValue() {
                final String deviceToken = deviceTokenComboBox.getValue();
                final String topic = topicComboBox.getValue();
                final String payload = payloadTextArea.getText();
                final DeliveryPriority deliveryPriority = deliveryPriorityComboBox.getValue();
                final String collapseId = collapseIdComboBox.getValue();

                final ApnsPushNotification pushNotification;

                if (StringUtils.isNoneBlank(deviceToken, topic, payload)) {
                    final Date expiration = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

                    pushNotification = new SimpleApnsPushNotification(deviceToken, topic, payload, expiration,
                            deliveryPriority, StringUtils.trimToNull(collapseId));
                } else {
                    pushNotification = null;
                }

                return pushNotification;
            }
        });
    }

    @SafeVarargs
    private static void addEmptyPseudoClassListener(final ComboBox<String>... comboBoxes) {
        for (final ComboBox<String> comboBox : comboBoxes) {
            comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                final boolean oldValueBlank = StringUtils.isBlank(oldValue);
                final boolean newValueBlank = StringUtils.isBlank(newValue);

                if (oldValueBlank != newValueBlank) {
                    comboBox.pseudoClassStateChanged(BLANK_PSEUDO_CLASS, newValueBlank);
                }
            });

            // Also set the psuedo class immediately based on the control's current state
            comboBox.pseudoClassStateChanged(BLANK_PSEUDO_CLASS, StringUtils.isBlank(comboBox.getValue()));
        }
    }

    private static void addEmptyPseudoClassListener(final TextInputControl... textInputControls) {
        for (final TextInputControl textInputControl : textInputControls) {
            textInputControl.textProperty().addListener((observable, oldValue, newValue) -> {
                final boolean oldValueBlank = StringUtils.isBlank(oldValue);
                final boolean newValueBlank = StringUtils.isBlank(newValue);

                if (oldValueBlank != newValueBlank) {
                    textInputControl.pseudoClassStateChanged(BLANK_PSEUDO_CLASS, newValueBlank);
                }
            });

            // Also set the psuedo class immediately based on the control's current state
            textInputControl.pseudoClassStateChanged(BLANK_PSEUDO_CLASS, StringUtils.isBlank(textInputControl.getText()));
        }
    }

    @FXML
    private void handleBrowseButtonAction(final ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("certificate-chooser.filter.pkcs8_and_pkcs12"), "*.p8", "*.p12"),
                new FileChooser.ExtensionFilter(resources.getString("certificate-chooser.filter.pkcs12"), "*.p12"),
                new FileChooser.ExtensionFilter(resources.getString("certificate-chooser.filter.pkcs8"), "*.p8"),
                new FileChooser.ExtensionFilter(resources.getString("certificate-chooser.filter.all"), "*.*"));

        final File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try {
                handleSigningKeyFileSelection(file);
            } catch (final NoSuchAlgorithmException | IOException | InvalidKeyException e) {
                // Couldn't load the given file as a signing key. Try it as a P12 certificate instead.
                final PasswordInputDialog passwordInputDialog = new PasswordInputDialog(password -> {
                    try {
                        CertificateUtil.getFirstPrivateKeyEntry(file, password);
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
                    try {
                        handleCertificateFileAndPasswordSelection(file, password);
                    } catch (final IOException | KeyStoreException | CertificateException e1) {
                        final Alert alert = new Alert(Alert.AlertType.WARNING);

                        alert.setTitle(resources.getString("alert.bad-certificate.title"));
                        alert.setHeaderText(resources.getString("alert.bad-certificate.header"));
                        alert.setContentText(resources.getString("alert.bad-certificate.content-text"));

                        alert.show();
                    }
                });
            }
        }
    }

    void handleSigningKeyFileSelection(final File signingKeyFile) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        ApnsSigningKey.loadFromPkcs8File(signingKeyFile, "temp", "temp");

        credentialsFileAndPasswordProperty.set(new Pair<>(signingKeyFile, null));

        final Matcher matcher = APNS_SIGNING_KEY_WITH_ID_PATTERN.matcher(signingKeyFile.getName());

        if (matcher.matches()) {
            if (StringUtils.isBlank(keyIdComboBox.getValue())) {
                keyIdComboBox.setValue(matcher.group(1));
                teamIdComboBox.requestFocus();
            }
        } else {
            keyIdComboBox.requestFocus();
        }
    }

    void handleCertificateFileAndPasswordSelection(final File certificateFile, final String password) throws IOException, KeyStoreException, CertificateException {
        if (CertificateUtil.extractApnsTopicsFromCertificate(certificateFile, password).isEmpty()) {
            throw new CertificateException("Certificate does not name any APNs topics.");
        }

        credentialsFileAndPasswordProperty.set(new Pair<>(certificateFile, password));
    }

    /**
     * Handles a successful attempt to send a push notification.
     */
    public void handleNotificationSent() {
        setRequiredFieldGroupHighlighted(false);

        final Preferences preferences = Preferences.userNodeForPackage(getClass());

        preferences.put(MOST_RECENT_SERVER_KEY, apnsServerComboBox.getValue());
        preferences.putInt(MOST_RECENT_PORT_KEY, apnsPortComboBox.getValue());
        preferences.put(MOST_RECENT_DELIVERY_PRIORITY_KEY, deliveryPriorityComboBox.getValue().name());

        if (StringUtils.isNotBlank(keyIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(keyIdComboBox);
        }

        if (StringUtils.isNotBlank(teamIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(teamIdComboBox);
        }

        addCurrentValueToComboBoxItems(topicComboBox);
        addCurrentValueToComboBoxItems(deviceTokenComboBox);

        if (StringUtils.isNotBlank(collapseIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(collapseIdComboBox);
        }

        final String payload = payloadTextArea.getText();

        recentPayloads.remove(payload);
        recentPayloads.add(0, payload);
    }

    private void savePreferencesList(final String key, final List<?> values) {
        Preferences.userNodeForPackage(getClass()).put(key, GSON.toJson(values));
    }

    private List<String> loadPreferencesList(final String key) {
        final Preferences preferences = Preferences.userNodeForPackage(getClass());

        try {
            return GSON.fromJson(preferences.get(key, "[]"), STRING_LIST_TYPE);
        } catch (final JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    private static<T> void addCurrentValueToComboBoxItems(final ComboBox<T> comboBox) {
        final T currentValue = comboBox.getValue();

        // Even if this item is already in the list, we want to make sure it gets moved to the top.
        comboBox.getItems().remove(currentValue);
        comboBox.getItems().add(0, currentValue);

        while (comboBox.getItems().size() > MAX_COMBO_BOX_ITEMS) {
            comboBox.getItems().remove(comboBox.getItems().size() - 1);
        }

        // In case the current value got deselected while modifying the list, make sure to reset the value.
        comboBox.setValue(currentValue);
    }

    /**
     * Returns the currently-selected APNs server.
     *
     * @return the currently-selected APNs server
     *
     * @see com.turo.pushy.apns.ApnsClientBuilder#PRODUCTION_APNS_HOST
     * @see com.turo.pushy.apns.ApnsClientBuilder#DEVELOPMENT_APNS_HOST
     */
    public final String getApnsServer() {
        return apnsServerWrapper.get();
    }

    /**
     * Returns the property representing the currently-selected APNs server.
     *
     * @return the property representing the currently-selected APNs server
     *
     * @see #getApnsServer()
     */
    public ReadOnlyStringProperty apnsServerProperty() {
        return apnsServerWrapper.getReadOnlyProperty();
    }

    /**
     * Returns the currently-selected APNs port.
     *
     * @return the currently-selected APNs port
     *
     * @see com.turo.pushy.apns.ApnsClientBuilder#DEFAULT_APNS_PORT
     * @see com.turo.pushy.apns.ApnsClientBuilder#ALTERNATE_APNS_PORT
     */
    public final int getApnsPort() {
        return apnsPortWrapper.get();
    }

    /**
     * Returns the property representing the currently-selected APNs port.
     *
     * @return the property representing the currently-selected APNs port
     *
     * @see #getApnsPort()
     */
    public ReadOnlyIntegerProperty apnsPortProperty() {
        return apnsPortWrapper.getReadOnlyProperty();
    }

    /**
     * Returns the user-selected APNs credentials.
     *
     * @return the user-selected APNs credentials, or an empty {@code Optional} if the credentials are either missing or
     * incomplete
     */
    public final Optional<ApnsCredentials> getApnsCredentials() {
        return Optional.ofNullable(apnsCredentialsWrapper.get());
    }

    /**
     * Returns the property representing the user-selected APNs credentials.
     *
     * @return the property representing the user-selected APNs credentials
     *
     * @see #getApnsCredentials()
     */
    public ReadOnlyObjectProperty<ApnsCredentials> apnsCredentialsProperty() {
        return apnsCredentialsWrapper.getReadOnlyProperty();
    }

    /**
     * Returns the push notification composed by the user. The composed push notification encompasses a topic,
     * destination device token, an optional "collapse ID," a delivery priority, and a payload.
     *
     * @return the push notification composed by the user, or an empty {@code Optional} if the user hasn't provided
     * values for all required fields
     */
    public final Optional<ApnsPushNotification> getPushNotification() {
        return Optional.ofNullable(pushNotificationWrapper.get());
    }

    /**
     * Returns the property representing the user-composed push notification.
     *
     * @return the property representing the user-composed push notification
     *
     * @see #getPushNotification()
     */
    public ReadOnlyObjectProperty<ApnsPushNotification> pushNotificationProperty() {
        return pushNotificationWrapper.getReadOnlyProperty();
    }

    /**
     * Sets whether fields that must be populated before sending a push notification should be highlighted.
     *
     * @param highlighted {@code true} if fields that must be populated before sending a push notification should be
     * highlighted or {@code false} otherwise
     */
    public final void setRequiredFieldGroupHighlighted(final boolean highlighted) {
        requiredFieldGroupHighlightedProperty.setValue(highlighted);
    }

    /**
     * Indicates whether fields that must be populated before sending a push notification are highlighted.
     *
     * @return {@code true} if fields that must be populated before sending a push notification are currently
     * highlighted or {@code false} otherwise
     */
    public final boolean isRequiredFieldGroupHighlighted() {
        return requiredFieldGroupHighlightedProperty.get();
    }

    /**
     * Returns the property representing whether fields that must be populated before sending a push notification are
     * highlighted.
     *
     * @return the property representing whether fields that must be populated before sending a push notification are
     * highlighted
     */
    public BooleanProperty requiredFieldGroupHighlightedProperty() {
        return requiredFieldGroupHighlightedProperty;
    }
}
