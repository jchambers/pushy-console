package com.turo.pushy.console;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CertificateUtilTest {

    private static final String CERTIFICATE_FILENAME = "apns-client.p12";
    private static final String CERTIFICATE_PASSWORD = "pushy-test";

    @Test
    public void testGetFirstPrivateKeyEntry() throws Exception {
        final File certificateFile = FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME));

        final KeyStore.PrivateKeyEntry privateKeyEntry =
                CertificateUtil.getFirstPrivateKeyEntry(certificateFile, CERTIFICATE_PASSWORD);

        assertNotNull(privateKeyEntry);
    }

    @Test
    public void testExtractApnsTopicsFromCertificate() throws Exception {
        final File certificateFile = FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME));
        final Set<String> topics = CertificateUtil.extractApnsTopicsFromCertificate(certificateFile, CERTIFICATE_PASSWORD);

        final Set<String> expectedTopics = new HashSet<>(Arrays.asList(
                "com.relayrides.pushy", "com.relayrides.pushy.voip", "com.relayrides.pushy.complication"));

        assertEquals(expectedTopics, topics);
    }
}