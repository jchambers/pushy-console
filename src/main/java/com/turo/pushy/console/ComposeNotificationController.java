package com.turo.pushy.console;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.console.util.CertificateUtil;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
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
 * A controller for the area of the area of the main window where users enter connection settings and notification
 * details.
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 */
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
    @FXML private MenuButton recentPayloadsMenuButton;
    @FXML private TextArea payloadTextArea;

    private final ObjectProperty<ApnsCredentialsFile> apnsCredentialsFileProperty = new SimpleObjectProperty<>();

    private final ListProperty<String> recentTopicsProperty = new SimpleListProperty<>();
    private final ObservableList<String> recentPayloads = FXCollections.observableArrayList();

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

    private static final PseudoClass EMPTY_PSEUDO_CLASS = PseudoClass.getPseudoClass("empty");

    public void initialize() {
        final Preferences preferences = Preferences.userNodeForPackage(getClass());

        this.apnsServerComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.PRODUCTION_APNS_HOST,
                ApnsClientBuilder.DEVELOPMENT_APNS_HOST));

        this.apnsServerComboBox.setValue(preferences.get(MOST_RECENT_SERVER_KEY, ApnsClientBuilder.PRODUCTION_APNS_HOST));

        this.apnsPortComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.DEFAULT_APNS_PORT,
                ApnsClientBuilder.ALTERNATE_APNS_PORT));

        this.apnsPortComboBox.setValue(preferences.getInt(MOST_RECENT_PORT_KEY, ApnsClientBuilder.DEFAULT_APNS_PORT));

        this.apnsCredentialFileTextField.textProperty().bind(new StringBinding() {
            {
                super.bind(ComposeNotificationController.this.apnsCredentialsFileProperty);
            }

            @Override
            protected String computeValue() {
                final ApnsCredentialsFile credentialsFile = ComposeNotificationController.this.apnsCredentialsFileProperty.get();
                return credentialsFile != null ? credentialsFile.getFile().getAbsolutePath() : null;
            }
        });

        this.deliveryPriorityComboBox.setCellFactory(listView -> new DeliveryPriorityListCell());
        this.deliveryPriorityComboBox.setButtonCell(new DeliveryPriorityListCell());

        this.deliveryPriorityComboBox.setItems(FXCollections.observableArrayList(
                DeliveryPriority.IMMEDIATE,
                DeliveryPriority.CONSERVE_POWER
        ));

        try {
            this.deliveryPriorityComboBox.setValue(DeliveryPriority.valueOf(preferences.get(MOST_RECENT_DELIVERY_PRIORITY_KEY, DeliveryPriority.IMMEDIATE.name())));
        } catch (final IllegalArgumentException e) {
            this.deliveryPriorityComboBox.setValue(DeliveryPriority.IMMEDIATE);
        }

        this.recentTopicsProperty.set(FXCollections.observableArrayList(loadPreferencesList(RECENT_TOPICS_KEY)));
        this.recentTopicsProperty.addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TOPICS_KEY, change.getList()));

        this.topicComboBox.itemsProperty().bind(this.recentTopicsProperty);

        this.apnsCredentialsFileProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isCertificate()) {
                // When working with certificates, we'll always have a fixed list of topics from the certificate and
                // should not allow freeform editing.
                this.topicComboBox.setEditable(false);

                this.topicComboBox.itemsProperty().unbind();

                this.topicComboBox.setItems(FXCollections.observableArrayList(newValue.getTopicsFromCertificate()));

                if (!this.topicComboBox.getItems().contains(this.topicComboBox.getValue())) {
                    this.topicComboBox.setValue(this.topicComboBox.getItems().get(0));
                }
            } else {
                this.topicComboBox.setEditable(true);
                this.topicComboBox.itemsProperty().bind(this.recentTopicsProperty);
            }
        });

        final BooleanBinding certificateCredentialsBinding = new BooleanBinding() {
            {
                super.bind(ComposeNotificationController.this.apnsCredentialsFileProperty);
            }

            @Override
            protected boolean computeValue() {
                final ApnsCredentialsFile credentialsFile = ComposeNotificationController.this.apnsCredentialsFileProperty.get();
                return credentialsFile != null && credentialsFile.isCertificate();
            }
        };

        this.keyIdLabel.disableProperty().bind(this.keyIdComboBox.disabledProperty());

        this.keyIdComboBox.disableProperty().bind(certificateCredentialsBinding);
        this.keyIdComboBox.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.keyIdComboBox.setValue(null);
            }
        });

        this.keyIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_KEY_IDS_KEY)));
        this.keyIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_KEY_IDS_KEY, change.getList()));

        this.teamIdLabel.disableProperty().bind(this.teamIdComboBox.disabledProperty());

        this.teamIdComboBox.disableProperty().bind(certificateCredentialsBinding);
        this.teamIdComboBox.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.teamIdComboBox.setValue(null);
            }
        });

        this.teamIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_TEAM_IDS_KEY)));
        this.teamIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TEAM_IDS_KEY, change.getList()));

        this.deviceTokenComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_TOKENS_KEY)));
        this.deviceTokenComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_TOKENS_KEY, change.getList()));

        this.collapseIdComboBox.setItems(FXCollections.observableArrayList(loadPreferencesList(RECENT_COLLAPSE_IDS_KEY)));
        this.collapseIdComboBox.getItems().addListener((ListChangeListener<String>) change ->
                savePreferencesList(RECENT_COLLAPSE_IDS_KEY, change.getList()));

        this.recentPayloads.addListener((ListChangeListener<String>) change -> {
            this.recentPayloadsMenuButton.getItems().clear();

            this.recentPayloadsMenuButton.getItems().addAll(change.getList().stream().map(payload -> {
                final String smooshedPayload = payload.replaceAll("\\s+", " ");
                final MenuItem menuItem = new MenuItem(smooshedPayload);

                menuItem.setOnAction(event -> this.payloadTextArea.setText(this.payloadTextArea.getText() + payload));

                return menuItem;
            }).collect(Collectors.toList()));

            this.recentPayloadsMenuButton.setDisable(change.getList().isEmpty());
        });

        this.recentPayloads.addAll(loadPreferencesList(RECENT_PAYLOADS_KEY));

        this.recentPayloads.addListener((ListChangeListener<String>) change -> {
            int end = this.recentPayloads.size();

            while (end > 0) {
                try {
                    preferences.put(RECENT_PAYLOADS_KEY, GSON.toJson(this.recentPayloads.subList(0, end)));
                    break;
                } catch (final IllegalArgumentException e) {
                    end -= 1;
                }
            }
        });

        addEmptyPseudoClassListener(this.keyIdComboBox, this.teamIdComboBox, this.topicComboBox, this.deviceTokenComboBox);
        addEmptyPseudoClassListener(this.apnsCredentialFileTextField, this.payloadTextArea);
    }

    @SafeVarargs
    private static void addEmptyPseudoClassListener(final ComboBox<String>... comboBoxes) {
        for (final ComboBox<String> comboBox : comboBoxes) {
            comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                final boolean oldValueBlank = StringUtils.isBlank(oldValue);
                final boolean newValueBlank = StringUtils.isBlank(newValue);

                if (oldValueBlank != newValueBlank) {
                    comboBox.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, newValueBlank);
                }
            });

            // Also set the psuedo class immediately based on the control's current state
            comboBox.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, StringUtils.isBlank(comboBox.getValue()));
        }
    }

    private static void addEmptyPseudoClassListener(final TextInputControl... textInputControls) {
        for (final TextInputControl textInputControl : textInputControls) {
            textInputControl.textProperty().addListener((observable, oldValue, newValue) -> {
                final boolean oldValueBlank = StringUtils.isBlank(oldValue);
                final boolean newValueBlank = StringUtils.isBlank(newValue);

                if (oldValueBlank != newValueBlank) {
                    textInputControl.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, newValueBlank);
                }
            });

            // Also set the psuedo class immediately based on the control's current state
            textInputControl.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, StringUtils.isBlank(textInputControl.getText()));
        }
    }

    @FXML
    protected void handleBrowseButtonAction(final ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(this.resources.getString("certificate-chooser.filter.pkcs8_and_pkcs12"), "*.p8", "*.p12"),
                new FileChooser.ExtensionFilter(this.resources.getString("certificate-chooser.filter.pkcs12"), "*.p12"),
                new FileChooser.ExtensionFilter(this.resources.getString("certificate-chooser.filter.pkcs8"), "*.p8"),
                new FileChooser.ExtensionFilter(this.resources.getString("certificate-chooser.filter.all"), "*.*"));

        final File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            try {
                ApnsSigningKey.loadFromPkcs8File(file, "temp", "temp");

                this.apnsCredentialsFileProperty.set(new ApnsCredentialsFile(file));

                final Matcher matcher = APNS_SIGNING_KEY_WITH_ID_PATTERN.matcher(file.getName());

                if (matcher.matches()) {
                    if (StringUtils.isBlank(this.keyIdComboBox.getValue())) {
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
                        this.apnsCredentialsFileProperty.set(new ApnsCredentialsFile(file, password));
                    } catch (final CertificateException e1) {
                        final Alert alert = new Alert(Alert.AlertType.WARNING);

                        alert.setTitle(this.resources.getString("alert.bad-certificate.title"));
                        alert.setHeaderText(this.resources.getString("alert.bad-certificate.header"));
                        alert.setContentText(this.resources.getString("alert.bad-certificate.content-text"));

                        alert.show();
                    } catch (final KeyStoreException | IOException e1) {
                        // This should never happen in practice since we checked the keystore as a means to verify the
                        // password earlier.
                        throw new RuntimeException(e1);
                    }
                });
            }
        }
    }

    void handleNotificationSent() {
        final Preferences preferences = Preferences.userNodeForPackage(getClass());

        preferences.put(MOST_RECENT_SERVER_KEY, this.apnsServerComboBox.getValue());
        preferences.putInt(MOST_RECENT_PORT_KEY, this.apnsPortComboBox.getValue());
        preferences.put(MOST_RECENT_DELIVERY_PRIORITY_KEY, this.deliveryPriorityComboBox.getValue().name());

        if (StringUtils.isNotBlank(this.keyIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(this.keyIdComboBox);
        }

        if (StringUtils.isNotBlank(this.teamIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(this.teamIdComboBox);
        }

        addCurrentValueToComboBoxItems(this.topicComboBox);
        addCurrentValueToComboBoxItems(this.deviceTokenComboBox);

        if (StringUtils.isNotBlank(this.collapseIdComboBox.getValue())) {
            addCurrentValueToComboBoxItems(this.collapseIdComboBox);
        }

        final String payload = this.payloadTextArea.getText();

        this.recentPayloads.remove(payload);
        this.recentPayloads.add(0, payload);
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

    boolean hasRequiredFields() {
        final boolean hasCredentials;

        final ApnsCredentialsFile credentialsFile = this.apnsCredentialsFileProperty.get();

        if (credentialsFile != null) {
            if (credentialsFile.isCertificate()) {
                hasCredentials = true;
            } else {
                final boolean hasKeyId = StringUtils.isNotBlank(this.keyIdComboBox.getValue());
                final boolean hasTeamId = StringUtils.isNotBlank(this.teamIdComboBox.getValue());

                hasCredentials = hasKeyId && hasTeamId;
            }
        } else {
            hasCredentials = false;
        }

        final boolean hasTopic = StringUtils.isNotBlank(this.topicComboBox.getValue());
        final boolean hasToken = StringUtils.isNotBlank(this.deviceTokenComboBox.getValue());
        final boolean hasPayload = StringUtils.isNotBlank(this.payloadTextArea.getText());

        return hasCredentials && hasTopic && hasToken && hasPayload;
    }

    ApnsClient buildClient() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final ApnsCredentialsFile credentialsFile = this.apnsCredentialsFileProperty.get();
        Objects.requireNonNull(credentialsFile, "APNs credentials must not be null.");

        final ApnsClientBuilder builder = new ApnsClientBuilder();

        builder.setApnsServer(this.apnsServerComboBox.getValue(), this.apnsPortComboBox.getValue());

        if (this.apnsCredentialsFileProperty.get().isCertificate()) {
            builder.setClientCredentials(credentialsFile.getFile(), credentialsFile.getCertificatePassword());
        } else {
            builder.setSigningKey(ApnsSigningKey.loadFromPkcs8File(credentialsFile.getFile(),
                    this.teamIdComboBox.getValue(), this.keyIdComboBox.getValue()));
        }

        return builder.build();
    }

    ApnsPushNotification buildPushNotification() {
        final String collapseId = StringUtils.trimToNull(this.collapseIdComboBox.getValue());

        return new SimpleApnsPushNotification(
                this.deviceTokenComboBox.getValue(),
                this.topicComboBox.getValue(),
                this.payloadTextArea.getText(),
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)),
                this.deliveryPriorityComboBox.getValue(),
                collapseId);
    }
}
