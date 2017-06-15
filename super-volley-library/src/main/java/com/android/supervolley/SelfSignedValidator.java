package com.android.supervolley;

import com.android.volley.VolleyLog;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;

/**
 * Checks whether given X.509 certificates are self-signed.
 */
class SelfSignedValidator implements CertificateValidator {

    private static final String TAG = SelfSignedValidator.class.getSimpleName();

    @Override
    public void validateCertificates(X509Certificate[] serverCertificates) throws CertificateException {
        boolean isSelfSigned = false;
        boolean isSignedWithCAParent = isSignedWithCAParent(serverCertificates);
        try {
            isSelfSigned = isSelfSigned(serverCertificates[0]);
        } catch (GeneralSecurityException e) {
            VolleyLog.d(TAG, "Something is invalid (key signature)");
        }
        if (isSelfSigned) {
            throw new CertificateException("The certificate is self-signed.");
        } else if (!isSignedWithCAParent) {
            throw new CertificateException("The certificate is not signed with one of his CA's.");
        }
    }

    /**
     * Verifies that this certificate was signed using the private key
     * that corresponds to the specified public key.
     */
    private boolean isSelfSigned(X509Certificate cert) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException sigEx) {
            // Invalid signature --> not self-signed
            return false;
        } catch (InvalidKeyException keyEx) {
            // Invalid key --> not self-signed
            return false;
        }
    }

    /**
     * Verifies that the leaf certificate was signed using one of the eligible CA
     */
    private boolean isSignedWithCAParent(X509Certificate[] certs) {
        X509Certificate cert = certs[0];
        for (int i = 1; i < certs.length; i++) {
            PublicKey key = certs[i].getPublicKey();
            try {
                cert.verify(key);
                return true;
            } catch (GeneralSecurityException e) {
                // do nothing, it's normal to throw an exception
                VolleyLog.d(TAG, "Something is invalid (key signature)");
            }
        }
        return false;
    }
}
