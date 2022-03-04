/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.InvalidCorrelationMessageException;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.MissingClientCredentialsException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
public class IdsResponseMessageFactoryTest {

    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private static final String TOKEN_VALUE = "xxxxx.yyyyy.zzzzz";
    private static final String CORRELATION_MESSAGE_ID = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.MESSAGE.getValue(), UUID.randomUUID().toString());
    private static final String CORRELATION_MESSAGE_SENDER = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), UUID.randomUUID().toString());
    private static final String CORRELATION_ISSUER_CONNECTOR = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), UUID.randomUUID().toString());

    private IdsResponseMessageFactory factory;

    // mocks
    private Message correlationMessage;
    private IdentityService identityService;

    @BeforeEach
    public void setup() {
        identityService = Mockito.mock(IdentityService.class);
        correlationMessage = Mockito.mock(Message.class);

        factory = new IdsResponseMessageFactory(CONNECTOR_ID, identityService);

        Mockito.when(correlationMessage.getId()).thenReturn(URI.create(CORRELATION_MESSAGE_ID));
        Mockito.when(correlationMessage.getSenderAgent()).thenReturn(URI.create(CORRELATION_MESSAGE_SENDER));
        Mockito.when(correlationMessage.getIssuerConnector()).thenReturn(URI.create(CORRELATION_ISSUER_CONNECTOR));

        Mockito.when(identityService.obtainClientCredentials(IdsClientCredentialsScope.ALL))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(TOKEN_VALUE).build()));
    }

    @Test
    public void testUrnConnectorIdSuccess() {
        String connectorUrn = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), CONNECTOR_ID);
        IdsResponseMessageFactory factory = new IdsResponseMessageFactory(connectorUrn, identityService);
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);
        Assertions.assertEquals(connectorUrn, message.getSenderAgent().toString());
    }

    @Test
    public void testUrnConnectorIdFailure() {
        String urn = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONTRACT_OFFER.getValue(), CONNECTOR_ID);

        Assertions.assertThrows(EdcException.class, () -> new IdsResponseMessageFactory(urn, identityService));
    }

    @Test
    public void testMessageId() {
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        IdsId messageId = IdsIdParser.parse(message.getId().toString());
        Assertions.assertNotNull(messageId);
        Assertions.assertEquals(IdsType.MESSAGE, messageId.getType());
    }

    @Test
    public void testSenderAgent() {
        String expectedSenderAgent = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), CONNECTOR_ID);

        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        Assertions.assertEquals(expectedSenderAgent, message.getSenderAgent().toString());
    }

    @Test
    public void testCorrelationMessageId() {
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        Assertions.assertEquals(CORRELATION_MESSAGE_ID, message.getCorrelationMessage().toString());
    }

    @Disabled // TODO enable with IDS-Serializer from issue 236
    @Test
    public void testIssued() {

        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        boolean isOlderThanNowMinus5Sec = Instant.now().getEpochSecond() - 5 < message.getIssued().toGregorianCalendar().toZonedDateTime().toEpochSecond();
        boolean isNewerThanNowPlus5Sec = Instant.now().getEpochSecond() + 5 > message.getIssued().toGregorianCalendar().toZonedDateTime().toEpochSecond();

        Assertions.assertTrue(isOlderThanNowMinus5Sec && isNewerThanNowPlus5Sec);
    }

    @Test
    public void testSecurityTokenValue() {
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        Assertions.assertEquals(TOKEN_VALUE, message.getSecurityToken().getTokenValue());
    }

    @Test
    public void testSecurityTokenFormat() {
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        Assertions.assertEquals(TokenFormat.JWT, message.getSecurityToken().getTokenFormat());
    }

    @Test
    public void testModelVersion() {
        NotificationMessage message = factory.createRequestInProcessMessage(correlationMessage);

        Assertions.assertEquals(IdsProtocol.INFORMATION_MODEL_VERSION, message.getModelVersion());
    }

    @Test
    public void testCorrelationMessageNull() {
        Mockito.when(correlationMessage.getId()).thenReturn(null);
        Assertions.assertThrows(InvalidCorrelationMessageException.class, () -> factory.createRequestInProcessMessage(correlationMessage));
    }

    @Test
    public void testSenderAgentNull() {
        Mockito.when(correlationMessage.getSenderAgent()).thenReturn(null);
        Assertions.assertThrows(InvalidCorrelationMessageException.class, () -> factory.createRequestInProcessMessage(correlationMessage));
    }

    @Test
    public void testIssuerConnectorNull() {
        Mockito.when(correlationMessage.getIssuerConnector()).thenReturn(null);
        Assertions.assertThrows(InvalidCorrelationMessageException.class, () -> factory.createRequestInProcessMessage(correlationMessage));
    }

    @Test
    public void testClientCredentialsMissing() {
        Mockito.when(identityService.obtainClientCredentials(IdsClientCredentialsScope.ALL)).thenReturn(Result.failure("foo"));
        Assertions.assertThrows(MissingClientCredentialsException.class, () -> factory.createRequestInProcessMessage(correlationMessage));
    }
}
