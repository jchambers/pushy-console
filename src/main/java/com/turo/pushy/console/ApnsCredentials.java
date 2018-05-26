package com.turo.pushy.console;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Objects;

public class ApnsCredentials {

    private final File credentialsFile;

    private final String certificatePassword;

    private final String keyId;
    private final String teamId;

    public ApnsCredentials(final File certificateFile, final String certificatePassword) {
        Objects.requireNonNull(certificateFile, "Certificate file must not be null.");
        Objects.requireNonNull(certificatePassword, "Certificate password may be blank, but must not be null.");

        this.credentialsFile = certificateFile;
        this.certificatePassword = certificatePassword;

        this.keyId = null;
        this.teamId = null;
    }

    public ApnsCredentials(final File signingKeyFile, final String keyId, final String teamId) {
        Objects.requireNonNull(signingKeyFile, "Signing key file must not be null.");

        if (StringUtils.isBlank(keyId)) {
            throw new IllegalArgumentException("Key ID must not be blank.");
        }

        if (StringUtils.isBlank(teamId)) {
            throw new IllegalArgumentException("Team ID must not be blank.");
        }

        this.credentialsFile = signingKeyFile;

        this.keyId = keyId;
        this.teamId = teamId;

        this.certificatePassword = null;
    }

    public File getCredentialsFile() {
        return credentialsFile;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getTeamId() {
        return teamId;
    }

    public boolean isCertificate() {
        return certificatePassword != null;
    }
}
