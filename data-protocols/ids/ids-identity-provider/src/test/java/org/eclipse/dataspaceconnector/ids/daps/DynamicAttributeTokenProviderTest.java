package org.eclipse.dataspaceconnector.ids.daps;

import java.time.Instant;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DynamicAttributeTokenProviderTest {

    // mocks
    private DapsClient dapsClient;

    @BeforeEach
    public void setup() {
        dapsClient = EasyMock.createMock(DapsClient.class);
    }

    @AfterEach
    public void teardown() {
        EasyMock.verify(dapsClient);
    }


    @Test
    public void testNullPointerExceptionOnClientArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeTokenProvider(null);
        });
    }

    @Test
    public void testWrapsExceptionInDapsClientException() {
        // prepare
        DynamicAttributeTokenProvider provider = new DynamicAttributeTokenProvider(dapsClient);

        EasyMock.expect(dapsClient.requestDynamicAttributeToken()).andThrow(new DapsClientException(""));
        EasyMock.replay(dapsClient);

        // invoke & verify
        Assertions.assertThrows(DynamicAttributeTokenException.class, provider::getDynamicAttributeToken);
    }
}
