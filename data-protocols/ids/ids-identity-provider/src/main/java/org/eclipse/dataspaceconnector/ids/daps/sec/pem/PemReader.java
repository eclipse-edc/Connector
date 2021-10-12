package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class PemReader {
    protected PemObject read(final InputStream inputStream) throws IOException {
        try (final PEMParser pemParser = new PEMParser(new InputStreamReader(inputStream))) {
            return pemParser.readPemObject();
        }
    }
}
