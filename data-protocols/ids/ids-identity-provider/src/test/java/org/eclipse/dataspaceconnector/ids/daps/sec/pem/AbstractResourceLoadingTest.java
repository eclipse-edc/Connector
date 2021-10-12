package org.eclipse.dataspaceconnector.ids.daps.sec.pem;

import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractResourceLoadingTest {
    private final List<InputStream> streams = new LinkedList<>();

    @AfterEach
    void closeStreams() {
        streams.forEach(this::closeSilently);
        streams.clear();
    }

    private void closeSilently(final InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {}
    }


    protected InputStream getResource(final String location) {
        final InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(location);
        streams.add(inputStream);
        return inputStream;
    }
}
