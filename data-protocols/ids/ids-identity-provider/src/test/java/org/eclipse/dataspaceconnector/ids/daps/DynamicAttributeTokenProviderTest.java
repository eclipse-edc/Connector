package org.eclipse.dataspaceconnector.ids.daps;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClient;
import org.eclipse.dataspaceconnector.ids.daps.client.DapsClientException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DynamicAttributeTokenProviderTest {

    @Test
    void testNullPointerExceptionOnClientArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeTokenProvider(null);
        });
    }

    @Test
    void testWrapsExceptionInDapsClientException() {
        // prepare
        DapsClient dapsClient = EasyMock.createMock(DapsClient.class);
        DynamicAttributeTokenProvider provider = new DynamicAttributeTokenProvider(dapsClient);

        EasyMock.expect(dapsClient.requestDynamicAttributeToken()).andThrow(new DapsClientException(""));
        EasyMock.replay(dapsClient);

        // invoke & verify
        Assertions.assertThrows(DynamicAttributeTokenException.class, provider::getDynamicAttributeToken);

        EasyMock.verify(dapsClient);
    }
}
