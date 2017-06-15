package com.android.supervolley;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks if the CA's keyUsage are set to TRUE.
 */
class CAKeyCertSignValidator implements CertificateValidator {

    @Override
    public void validateCertificates(X509Certificate[] serverCertificates) throws CertificateException {
        List<Boolean> validCerts = new ArrayList<>();
        for (X509Certificate certificate : serverCertificates) {
            // The basic constraints extension identifies whether the subject of the certificate is
            // a Certificate Authority (CA) and how deep a certification path may exist through that CA
            if (certificate.getBasicConstraints() != -1) {
                // keyCertSign (5) -> Should be true for any CA.
                validCerts.add(certificate.getKeyUsage()[5]);
            }
        }

        if (validCerts.contains(false)) {
            throw new CertificateException("Invalid CA: false");
        }
    }
}
