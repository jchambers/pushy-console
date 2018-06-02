package com.turo.pushy.console;

import com.turo.pushy.apns.auth.ApnsSigningKey;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ApnsCredentialsTest {

    private static final String CERTIFICATE_FILENAME = "apns-client.p12";
    private static final String CERTIFICATE_PASSWORD = "pushy-test";

    private static final String SIGNING_KEY_FILENAME = "APNsAuthKey_KEYIDKEYID.p8";

    @Test
    public void testCertificateCredentials() throws Exception {
        final File certificateFile = FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME));

        final ApnsCredentials certificateCredentials = new ApnsCredentials(certificateFile, CERTIFICATE_PASSWORD);

        assertTrue(certificateCredentials.getCertificateAndPrivateKey().isPresent());
        assertFalse(certificateCredentials.getSigningKey().isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void testCertificateCredentialsNullFile() throws Exception {
        new ApnsCredentials(null, CERTIFICATE_PASSWORD);
    }

    @Test(expected = NullPointerException.class)
    public void testCertificateCredentialsNullPassword() throws Exception {
        new ApnsCredentials(FileUtils.toFile(getClass().getResource(CERTIFICATE_FILENAME)), null);
    }

    @Test
    public void testSigningKeyCredentials() throws Exception {
        final File signingKeyFile = FileUtils.toFile(getClass().getResource(SIGNING_KEY_FILENAME));

        final String keyId = "KEYID";
        final String teamId = "TEAMID";

        final ApnsCredentials signingKeyCredentials = new ApnsCredentials(signingKeyFile, keyId, teamId);

        assertFalse(signingKeyCredentials.getCertificateAndPrivateKey().isPresent());
        assertTrue(signingKeyCredentials.getSigningKey().isPresent());

        final ApnsSigningKey signingKey = signingKeyCredentials.getSigningKey().get();

        assertEquals(keyId, signingKey.getKeyId());
        assertEquals(teamId, signingKey.getTeamId());
    }

    @Test(expected = NullPointerException.class)
    public void testSigningKeyCredentialsNullFile() throws Exception {
        new ApnsCredentials(null, "KEYID", "TEAMID");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSigningKeyCredentialsNullKeyId() throws Exception {
        new ApnsCredentials(FileUtils.toFile(getClass().getResource(SIGNING_KEY_FILENAME)), null, "TEAMID");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSigningKeyCredentialsNullTeamId() throws Exception {
        new ApnsCredentials(FileUtils.toFile(getClass().getResource(SIGNING_KEY_FILENAME)), "KEYID", null);
    }
}