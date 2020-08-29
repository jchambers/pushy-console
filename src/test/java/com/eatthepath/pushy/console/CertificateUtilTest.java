/*
 * Copyright (c) 2020 Jon Chambers.
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

package com.eatthepath.pushy.console;

import com.eatthepath.pushy.console.CertificateUtil;
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