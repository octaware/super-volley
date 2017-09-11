package com.android.supervolley;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Pinning against the public key.
 */
class RootCAPublicKeyValidator implements CertificateValidator {

    private List<String> publicKeys;

    RootCAPublicKeyValidator(String[] publicKeys) {
        this.publicKeys = Arrays.asList(publicKeys);
    }

    @Override
    public void validateCertificates(X509Certificate[] serverCertificates) throws CertificateException {
        X509Certificate root = serverCertificates[serverCertificates.length - 1];
        if (publicKeys.isEmpty()) {
            // skip the public key check if the user did not set any
            return;
        }
        boolean isValid = publicKeys.contains(root.getPublicKey().toString());
        if (!isValid) {
            throw new CertificateException("Public key doesn't match.");
        }
    }
}
