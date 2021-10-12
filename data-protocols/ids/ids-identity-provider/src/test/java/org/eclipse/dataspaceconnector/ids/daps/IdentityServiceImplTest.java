package org.eclipse.dataspaceconnector.ids.daps;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class IdentityServiceImplTest {

    private static final String TOKEN = "132456789";

    // mocks
    private Monitor monitor;
    private DynamicAttributeToken dynamicAttributeToken;
    private DynamicAttributeTokenProvider dynamicAttributeTokenProvider;


    @BeforeEach
    void setup() throws DynamicAttributeTokenException {
        monitor = EasyMock.createMock(Monitor.class);
        dynamicAttributeToken = EasyMock.createMock(DynamicAttributeToken.class);
        dynamicAttributeTokenProvider = EasyMock.createMock(DynamicAttributeTokenProvider.class);
        EasyMock.expect(dynamicAttributeToken.getToken()).andReturn(TOKEN).anyTimes();
        EasyMock.expect(dynamicAttributeToken.getExpiration()).andReturn(Instant.now()).anyTimes();
        EasyMock.expect(dynamicAttributeTokenProvider.getDynamicAttributeToken()).andReturn(dynamicAttributeToken).anyTimes();

        EasyMock.replay(monitor, dynamicAttributeToken, dynamicAttributeTokenProvider);
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(monitor, dynamicAttributeTokenProvider);
    }

    @Test
    void testObtainClientCredentials() {
        // prepare
        final IdentityService identityService = new IdentityServiceImpl(dynamicAttributeTokenProvider, monitor);

        // invoke
        final TokenResult result = identityService.obtainClientCredentials("scopes not implemented yet");

        // verify
        Assertions.assertEquals(TOKEN, result.getToken());
    }
}
