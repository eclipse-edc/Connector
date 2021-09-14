package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QueryRequestTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        QueryRequest process = QueryRequest.Builder.newInstance().protocol("foo").connectorId("id").connectorAddress("https://localhost:8080").queryLanguage("bar").query("baz").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        QueryRequest deserialized = mapper.readValue(writer.toString(), QueryRequest.class);

        assertNotNull(deserialized);
    }


}
