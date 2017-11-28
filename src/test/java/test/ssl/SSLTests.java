/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.ssl;

import com.franz.agraph.repository.AGServer;
import org.junit.Test;
import test.AGAbstractTest;
import test.Util;

import java.io.File;

/**
 * Tests X.509 authentication
 * <p>
 * Set SSL directives in the server's agraph.cfg file, e.g:
 * <p>
 * SSLPort 10036
 * SSLClientAuthRequired true
 * SSLClientAuthUsernameField CN
 * SSLCertificate /path/agraph.cert
 * SSLCAFile /path/ca.cert
 */
public class SSLTests extends AGAbstractTest {
    @Test
    public void x509test() throws Exception {
        final File defaultKeyStore = Util.resourceAsTempFile("/test/ssl/test.p12");
        String ks = System.getProperty(
                "javax.net.ssl.keyStore",
                defaultKeyStore.getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStore", ks);

        System.setProperty("javax.net.ssl.keyStorePassword", "foobar");
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

        final File defaultTrustStore = Util.resourceAsTempFile("/test/ssl/ts");
        String ts = System.getProperty(
                "javax.net.ssl.trustStore",
                defaultTrustStore.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStore", ts);

        server = new AGServer(findSslServerUrl());
        server.listCatalogs();
    }
}
