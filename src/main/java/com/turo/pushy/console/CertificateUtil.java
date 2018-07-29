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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CertificateUtil {
    private static final String TOPIC_OID = "1.2.840.113635.100.6.3.6";

    // Borrowed with gratitude from https://github.com/aerogear/aerogear-unifiedpush-server
    private static final Pattern UID_PATTERN = Pattern.compile(".*UID=([^,]+).*");
    private static final Pattern COMMON_NAME_PATTERN = Pattern.compile("CN=(.*?):");
    private static final Set<String> APNS_COMMON_NAMES = new HashSet<>(Arrays.asList(
            "Apple Push Services",  "Apple Production IOS Push Services", "Apple Development IOS Push Services",
            "Pass Type ID"));

    /**
     * <p>Returns a private key entry from the given PKCS#12 key store that appears to contain valid APNs client
     * credentials. A private key entry contains valid APNs client if:</p>
     *
     * <ol>
     *     <li>It contains a certificate that has a common name that indicates it is intended for use as APNs client
     *     credentials.</li>
     *     <li>The certificate, either through a UID entry in its distinguished name or in certificate extensions,
     *     identifies at least one APNs topic.</li>
     * </ol>
     *
     * @param p12File the file from which to load a private key entry
     * @param password the password to unlock the given file
     *
     * @return A private key entry from the given PKCS#12 file that is valid for use as APNs client credentials. If the
     * file contains multiple valid private key entries, which one is returned is undefined.
     *
     * @throws KeyStoreException if a valid private key entry could not be extracted from the given file for any reason
     * @throws IOException if the given file could not be read for any reason
     */
    static KeyStore.PrivateKeyEntry getFirstPrivateKeyEntry(final File p12File, final String password) throws KeyStoreException, IOException {
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
                final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

                final String subjectName =
                        ((X509Certificate) privateKeyEntry.getCertificate()).getSubjectX500Principal().getName();

                final Matcher commonNameMatcher = COMMON_NAME_PATTERN.matcher(subjectName);

                while (commonNameMatcher.find()) {
                    if (APNS_COMMON_NAMES.contains(commonNameMatcher.group(1))) {
                        // The certificate has a common name that we'd expect of APNs client credentials

                        if (!extractApnsTopicsFromCertificate(privateKeyEntry.getCertificate()).isEmpty()) {
                            // …and it appears to name at least one APNs topic.
                            return privateKeyEntry;
                        }
                    }
                }
            }
        }

        throw new KeyStoreException("Key store did not contain any private key entries suitable for use as APNs client credentials.");
    }

    /**
     * Extracts a list of supported APNs topics from an APNs client credential certificate in the given PKCS#12 key
     * store.
     *
     * @param certificateFile the file from which to extract a set of topics
     * @param password the password to unlock the given file
     *
     * @return a set of APNs topics supported by the first APNs credential certificate found in the given keystore; if
     * more than one valid certificate is found in the file, which set of topics returned is undefined
     *
     * @throws KeyStoreException if a set of topics could not be extracted from the given file for any reason
     * @throws IOException if the given file could not be read for any reason
     */
    static Set<String> extractApnsTopicsFromCertificate(final File certificateFile, final String password) throws IOException, KeyStoreException {
        final KeyStore.PrivateKeyEntry privateKeyEntry = getFirstPrivateKeyEntry(certificateFile, password);
        return extractApnsTopicsFromCertificate(privateKeyEntry.getCertificate());
    }

    /**
     * Extracts a list of supported APNs topics from the given certificate.
     *
     * @param certificate the certificate from which to extract a set of APNs topics
     *
     * @return a set of APNs topics supported by the given certificate
     *
     * @throws IOException if the given certificate could not be parsed for any reason
     */
    private static Set<String> extractApnsTopicsFromCertificate(final Certificate certificate) throws IOException {
        final Set<String> topics = new HashSet<>();

        if (certificate instanceof X509Certificate) {
            final X509Certificate x509Certificate = (X509Certificate) certificate;

            final Matcher uidMatcher = UID_PATTERN.matcher(x509Certificate.getSubjectX500Principal().getName());

            while (uidMatcher.find()) {
                topics.add(uidMatcher.group(1));
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
