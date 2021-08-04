package org.eclipse.dataspaceconnector.transfer.demo.protocols.ws;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Wraps an inputstream so it is not closed.
 */
public class NonClosingInputStream extends FilterInputStream {

    public NonClosingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() {
        // suppress close
    }
}
