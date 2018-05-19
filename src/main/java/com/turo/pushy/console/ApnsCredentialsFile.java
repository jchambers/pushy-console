package com.turo.pushy.console;

import com.turo.pushy.console.util.CertificateUtil;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * An APNs credentials file represents a file that may be either a PKCS#8 private key or a PKCS#12-packaged certificate
 * and private key.
 */
public class ApnsCredentialsFile {

    private final File file;
    private final String certificatePassword;

    private transient final List<String> topicsFromCertificate;

    /**
     * Constructs a new credentials file that refers to a PKCS#8 private key.
     *
     * @param signingKeyFile the file containing the key; must not be {@code null}
     */
    public ApnsCredentialsFile(final File signingKeyFile) {
        Objects.requireNonNull(signingKeyFile, "Signing key file must not be null.");

        this.file = signingKeyFile;
        this.certificatePassword = null;

        this.topicsFromCertificate = null;
    }

    /**
     * Constructs a new credentials file that refers to a PKCS#12-packaged certificate and private key.
     *
     * @param certificateFile the PKCS#12 file containing the certificate and private key
     * @param certificatePassword the password needed to decrypt the PKCS#12 keystore
     *
     * @throws IOException if the given file could not be read for any reason
     * @throws KeyStoreException if a certificate/key pair could not be extracted from the given PKCS#12 keystore
     * @throws CertificateException if the given file does not contain a certificate appropriate for use as APNs client
     * credentials
     */
    public ApnsCredentialsFile(final File certificateFile, final String certificatePassword) throws IOException, KeyStoreException, CertificateException {
        Objects.requireNonNull(certificateFile, "Certificate file must not be null.");
        Objects.requireNonNull(certificatePassword, "Certificate password must not be null.");

        this.file = certificateFile;
        this.certificatePassword = certificatePassword;

        final KeyStore.PrivateKeyEntry privateKeyEntry = CertificateUtil.getFirstPrivateKeyEntry(certificateFile, certificatePassword);

        final List<String> topics = new ArrayList<>(CertificateUtil.extractApnsTopicsFromCertificate(privateKeyEntry.getCertificate()));

        if (topics.isEmpty()) {
            throw new CertificateException("Certificate does not name any APNs topics.");
        }

        topics.sort(Comparator.naturalOrder());
        this.topicsFromCertificate = Collections.unmodifiableList(topics);
    }

    /**
     * Checks whether this credentials file represents a certificate.
     *
     * @return {@code true} if this file represents a certificate or {@code false} if it represents a signing key
     */
    public boolean isCertificate() {
        return this.certificatePassword != null;
    }

    /**
     * Checks whether this credentials file represents a signing key.
     *
     * @return {@code true} if this file represents a signing key or {@code false} if it represents a certificate
     */
    public boolean isSigningKey() {
        return !this.isCertificate();
    }

    /**
     * Returns the underlying file containing APNs client credentials.
     *
     * @return the underlying file containing APNs client credentials
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Returns the password for decrypting the APNs client certificate in this file.
     *
     * @return the password for decrypting the APNs client certificate in this file, or {@code null} if this file
     * represents an APNs signing key
     */
    public String getCertificatePassword() {
        return this.certificatePassword;
    }

    /**
     * Returns a list of topics to which the APNs client certificate in this file may be used to send notifications.
     *
     * @return a list of topics to which the APNs client certificate in this file may be used to send notifications, or
     * {@code null} if this file represents an APNs signing key
     */
    public List<String> getTopicsFromCertificate() {
        return this.topicsFromCertificate;
    }
}
