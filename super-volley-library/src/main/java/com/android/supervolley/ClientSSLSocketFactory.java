package com.android.supervolley;

import android.net.SSLCertificateSocketFactory;

import com.android.volley.VolleyLog;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class ClientSSLSocketFactory {

    private static final String TAG = ClientSSLSocketFactory.class.getSimpleName();

    private static SSLSocketFactory socketFactory;
    private static X509TrustManager trustManager;
    private static CertificateValidator[] validators;
    private static boolean isSecured = Boolean.TRUE;
    private static String[] publicKeys;

    static SSLSocketFactory getSocketFactory() {
        if (socketFactory == null) {
            try {
                X509TrustManager trustManager = get509TrustManager();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                socketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                VolleyLog.e(TAG, "Unable to create the ssl socket factory.");
                return SSLCertificateSocketFactory.getDefault(0, null);
            }
        }
        return socketFactory;
    }

    static X509TrustManager get509TrustManager() {
        if (trustManager == null) {
            trustManager = new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    for (CertificateValidator validator : getValidators()) {
                        validator.validateCertificates(chain);
                    }
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
        }
        return trustManager;
    }

    private static CertificateValidator[] getValidators() {
        if (!isSecured) {
            return new CertificateValidator[0];
        }
        if (validators == null) {
            validators = new CertificateValidator[4];
            validators[0] = new CertificateValidityValidator();
            validators[1] = new CAKeyCertSignValidator();
            validators[2] = new SelfSignedValidator();
            validators[3] = new RootCAPublicKeyValidator(publicKeys);
        }
        return validators;
    }

    static void setIsSecured(boolean isSecured) {
        ClientSSLSocketFactory.isSecured = isSecured;
    }

    static void setPublicKeys(String[] publicKeys) {
        ClientSSLSocketFactory.publicKeys = publicKeys;
    }
}
