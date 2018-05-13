package com.turo.pushy.console.util;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class CertificateUtil {
    private static final String TOPIC_OID = "1.2.840.113635.100.6.3.6";

    public static KeyStore.PrivateKeyEntry getFirstPrivateKeyEntry(final File p12File, final String password) throws KeyStoreException, IOException {
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

        throw new KeyStoreException("Key store did not contain any private key entries suitable for use as APNs client credentials.");
    }

    public static Set<String> extractApnsTopicsFromCertificate(final Certificate certificate) throws IOException {
        final Set<String> topics = new HashSet<String>();

        if (certificate instanceof X509Certificate) {
            final X509Certificate x509Certificate = (X509Certificate) certificate;

            for (final String keyValuePair : x509Certificate.getSubjectX500Principal().getName().split(",")) {
                if (keyValuePair.toLowerCase().startsWith("uid=")) {
                    topics.add(keyValuePair.substring(4));
                    break;
                }
            }

            final byte[] topicExtensionData = x509Certificate.getExtensionValue(TOPIC_OID);

            if (topicExtensionData != null) {
                final ASN1Primitive extensionValue = JcaX509ExtensionUtils.parseExtensionValue(topicExtensionData);

                if (extensionValue instanceof ASN1Sequence) {
                    final ASN1Sequence sequence = (ASN1Sequence) extensionValue;

                    for (int i = 0; i < sequence.size(); i++) {
                        if (sequence.getObjectAt(i) instanceof ASN1String) {
                            topics.add(sequence.getObjectAt(i).toString());
                        }
                    }
                }
            }
        }

        return topics;
    }
}
