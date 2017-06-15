package com.android.supervolley;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Checks that the certificate is currently valid. It is if the current
 * date and time are within the validity period given in the certificate.
 */
class CertificateValidityValidator implements CertificateValidator {

    /**
     * @throws CertificateException if the certificate has expired or if the certificate is not yet valid.
     */
    @Override
    public void validateCertificates(X509Certificate[] serverCertificates) throws CertificateException {
        for (X509Certificate certificate : serverCertificates) {
            certificate.checkValidity();
        }
    }
}
