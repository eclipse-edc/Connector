package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okio.BufferedSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamingRequestBodyTest {
    private static final byte[] CONTENT = "123".getBytes();

    @Test
    void verifyStreamingTransfer() throws IOException {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(CONTENT));

        var sink = mock(BufferedSink.class);
        var outputStream = new ByteArrayOutputStream();

        when(sink.outputStream()).thenReturn(outputStream);

        var body = new StreamingRequestBody(part);
        body.writeTo(sink);

        assertThat(outputStream.toByteArray()).isEqualTo(CONTENT);
    }
}
