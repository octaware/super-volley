package com.android.supervolley;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

interface CertificateValidator {

    void validateCertificates(X509Certificate[] serverCertificates)
            throws CertificateException;
}
