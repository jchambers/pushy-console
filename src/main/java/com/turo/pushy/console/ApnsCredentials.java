package com.turo.pushy.console;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ApnsCredentials {

    private final ApnsCredentialsFile credentialsFile;

    private final String keyId;
    private final String teamId;

    public ApnsCredentials(final ApnsCredentialsFile certificateFile) {
        Objects.requireNonNull(certificateFile, "Certificate file must not be null.");

        if (!certificateFile.isCertificate()) {
            throw new IllegalArgumentException("Credentials file is not a certificate; use the signing key constructor instead.");
        }

        this.credentialsFile = certificateFile;

        this.keyId = null;
        this.teamId = null;
    }

    public ApnsCredentials(final ApnsCredentialsFile signingKeyFile, final String keyId, final String teamId) {
        Objects.requireNonNull(signingKeyFile, "Signing key file must not be null.");

        if (!signingKeyFile.isSigningKey()) {
            throw new IllegalArgumentException("Credentials file is not a signing key; use certificate constructor instead.");
        }

        if (StringUtils.isBlank(keyId)) {
            throw new IllegalArgumentException("Key ID must not be blank.");
        }

        if (StringUtils.isBlank(teamId)) {
            throw new IllegalArgumentException("Team ID must not be blank.");
        }

        this.credentialsFile = signingKeyFile;

        this.keyId = keyId;
        this.teamId = teamId;
    }

    public ApnsCredentialsFile getCredentialsFile() {
        return credentialsFile;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getTeamId() {
        return teamId;
    }
}
