package com.turo.pushy.console;

import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposeNotificationController implements Initializable {

    @FXML private ComboBox<String> apnsServerComboBox;
    @FXML private ComboBox<Integer> apnsPortComboBox;
    @FXML private TextField apnsCredentialFileTextField;

    @FXML private Label keyIdLabel;
    @FXML private TextField keyIdTextField;
    @FXML private Label teamIdLabel;
    @FXML private TextField teamIdTextField;

    @FXML private ComboBox<String> deliveryPriorityComboBox;

    private String certificatePassword;

    private static final Pattern APNS_SIGNING_KEY_WITH_ID_PATTERN =
            Pattern.compile("^APNsAuthKey_([A-Z0-9]{10}).p8$", Pattern.CASE_INSENSITIVE);

    public void initialize(final URL location, final ResourceBundle resources) {
        this.apnsServerComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.DEVELOPMENT_APNS_HOST,
                ApnsClientBuilder.PRODUCTION_APNS_HOST));

        this.apnsServerComboBox.setValue(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);

        this.apnsPortComboBox.setItems(FXCollections.observableArrayList(
                ApnsClientBuilder.DEFAULT_APNS_PORT,
                ApnsClientBuilder.ALTERNATE_APNS_PORT));

        this.apnsPortComboBox.setValue(ApnsClientBuilder.DEFAULT_APNS_PORT);

        // TODO Localize
        this.deliveryPriorityComboBox.setItems(FXCollections.observableArrayList(
                "Immediate",
                "Conserve power"
        ));

        this.deliveryPriorityComboBox.setValue("Immediate");
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
                this.keyIdTextField.setDisable(false);
                this.teamIdLabel.setDisable(false);
                this.teamIdTextField.setDisable(false);

                final Matcher matcher = APNS_SIGNING_KEY_WITH_ID_PATTERN.matcher(file.getName());

                if (matcher.matches()) {
                    this.keyIdTextField.setText(matcher.group(1));
                    this.teamIdTextField.requestFocus();
                } else {
                    this.keyIdTextField.requestFocus();
                }
            } catch (final NoSuchAlgorithmException | IOException | InvalidKeyException e) {
                // Couldn't load the given file as a signing key. Try it as a P12 certificate instead.
                final Optional<String> verifiedPassword = new PasswordInputDialog(password -> {
                    try {
                        return checkPasswordForCertificate(file, password);
                    } catch (final KeyStoreException | IOException e1) {
                        return false;
                    }
                }).showAndWait();

                verifiedPassword.ifPresent(password -> {
                    this.certificatePassword = password;
                    this.apnsCredentialFileTextField.setText(file.getAbsolutePath());

                    this.keyIdTextField.clear();
                    this.teamIdTextField.clear();

                    this.keyIdLabel.setDisable(true);
                    this.keyIdTextField.setDisable(true);
                    this.teamIdLabel.setDisable(true);
                    this.teamIdTextField.setDisable(true);
                });
            }
        }
    }

    private boolean checkPasswordForCertificate(final File certificateFile, final String password) throws KeyStoreException, IOException {
        final char[] passwordCharacters = password.toCharArray();
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (final FileInputStream certificateInputStream = new FileInputStream(certificateFile)) {
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
                return true;
            }
        }

        throw new KeyStoreException("Key store did not contain any private key entries.");
    }
}
