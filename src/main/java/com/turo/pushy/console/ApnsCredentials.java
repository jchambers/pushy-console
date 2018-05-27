package com.turo.pushy.console;

import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.console.util.CertificateUtil;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

/**
 * A set of APNs client credentials that may contain exactly one of an APNs client certificate/private key pair or an
 * APNs signing key.
 */
public class ApnsCredentials {

    private final Pair<X509Certificate, PrivateKey> certificateAndPrivateKey;

    private final ApnsSigningKey signingKey;

    /**
     * Constructs a new set of APNs client credentials that contains the given certificate/private key pair.
     *
     * @param certificateFile a PKCS#12 file containing the certificate and private key
     * @param certificatePassword the password for the PKCS#12 file
     *
     * @throws IOException if the given file could not be read for any reason
     * @throws KeyStoreException if a certificate/private key pair could not be loaded from the given PKCS#12 file for
     * any reason
     */
    public ApnsCredentials(final File certificateFile, final String certificatePassword) throws IOException, KeyStoreException {
        Objects.requireNonNull(certificateFile, "Certificate file must not be null.");
        Objects.requireNonNull(certificatePassword, "Certificate password may be blank, but must not be null.");

        final KeyStore.PrivateKeyEntry privateKeyEntry =
                CertificateUtil.getFirstPrivateKeyEntry(certificateFile, certificatePassword);

        certificateAndPrivateKey = new Pair<>((X509Certificate) privateKeyEntry.getCertificate(), privateKeyEntry.getPrivateKey());
        signingKey = null;
    }

    /**
     * Constructs a new set of APNs client credentials that contains the given signing key.
     *
     * @param signingKeyFile a PKCS#8 file that contains an EC private key
     * @param keyId the ten-character, Apple-issued ID for the signing key
     * @param teamId the ten-character, Apple-issued ID for the team to which the signing key belongs
     *
     * @throws NoSuchAlgorithmException if the JVM does not support elliptic curve keys
     * @throws IOException if the given PKCS#8 file could not be read for any reason
     * @throws InvalidKeyException if the given elliptic curve private key is invalid for any reason
     */
    public ApnsCredentials(final File signingKeyFile, final String keyId, final String teamId) throws NoSuchAlgorithmException, IOException, InvalidKeyException {
        Objects.requireNonNull(signingKeyFile, "Signing key file must not be null.");

        if (StringUtils.isBlank(keyId)) {
            throw new IllegalArgumentException("Key ID must not be blank.");
        }

        if (StringUtils.isBlank(teamId)) {
            throw new IllegalArgumentException("Team ID must not be blank.");
        }

        signingKey = ApnsSigningKey.loadFromPkcs8File(signingKeyFile, teamId, keyId);
        certificateAndPrivateKey = null;
    }

    /**
     * Returns the certificate and private key pair (if present) contained in this set of APNs client credentials. If
     * the returned {@code Optional} has a value, the {@code Optional} returned by {@link #getSigningKey()} is
     * guaranteed to be empty.
     *
     * @return an {@code Optional} containing the certificate and private key pair for this set of APNs client
     * credentials
     */
    public Optional<Pair<X509Certificate, PrivateKey>> getCertificateAndPrivateKey() {
        return Optional.ofNullable(certificateAndPrivateKey);
    }

    /**
     * Returns the signing key (if present) contained in this set of APNs client credentials. If the returned
     * {@code Optional} has a value, the {@code Optional} returned by {@link #getCertificateAndPrivateKey()} is
     * guaranteed to be empty.
     *
     * @return an {@code Optional} containing the signing key for this set of APNs client credentials
     */
    public Optional<ApnsSigningKey> getSigningKey() {
        return Optional.ofNullable(signingKey);
    }
}
