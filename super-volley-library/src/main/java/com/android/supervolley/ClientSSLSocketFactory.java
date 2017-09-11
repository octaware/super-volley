package com.android.supervolley;

import android.net.SSLCertificateSocketFactory;

import com.android.volley.VolleyLog;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class ClientSSLSocketFactory {

    private static final String TAG = ClientSSLSocketFactory.class.getSimpleName();

    private static ClientSSLSocketFactory instance;

    private SSLSocketFactory socketFactory;
    private X509TrustManager trustManager;
    private CertificateValidator[] validators;
    private boolean isSecured = Boolean.TRUE;
    private String[] publicKeys;

    private ClientSSLSocketFactory() {
    }

    static ClientSSLSocketFactory sslSocketFactory() {
        if (instance == null)
            instance = new ClientSSLSocketFactory();
        return instance;
    }

    void init(boolean isSecured, String[] publicKeys) {
        this.isSecured = isSecured;
        this.publicKeys = publicKeys;
    }

    SSLSocketFactory getFactory() {
        if (socketFactory == null) {
            try {
                X509TrustManager manager = get509TrustManager();
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{manager}, null);
                socketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                VolleyLog.e(TAG, "Unable to create the ssl socket factory.");
                return SSLCertificateSocketFactory.getDefault(0, null);
            }
        }
        return socketFactory;
    }

    X509TrustManager get509TrustManager() {
        if (trustManager == null) {
            trustManager = new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    for (CertificateValidator validator : getValidators()) {
                        validator.validateCertificates(chain);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
        }
        return trustManager;
    }

    private CertificateValidator[] getValidators() {
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
}
