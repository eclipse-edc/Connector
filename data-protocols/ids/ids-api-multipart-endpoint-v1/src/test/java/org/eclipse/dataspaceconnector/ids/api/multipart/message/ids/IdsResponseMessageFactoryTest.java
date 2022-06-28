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
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.TokenFormat;
import jakarta.inject.Provider;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.InvalidCorrelationMessageException;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions.MissingClientCredentialsException;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.argThat;

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
    private InvalidCorrelationMessageException exception;

    @BeforeEach
    public void setup() {
        identityService = Mockito.mock(IdentityService.class);
        correlationMessage = Mockito.mock(Message.class);
        exception = Mockito.mock(InvalidCorrelationMessageException.class);

        factory = new IdsResponseMessageFactory(CONNECTOR_ID, identityService);

        Mockito.when(correlationMessage.getId()).thenReturn(URI.create(CORRELATION_MESSAGE_ID));
        Mockito.when(correlationMessage.getSenderAgent()).thenReturn(URI.create(CORRELATION_MESSAGE_SENDER));
        Mockito.when(correlationMessage.getIssuerConnector()).thenReturn(URI.create(CORRELATION_ISSUER_CONNECTOR));

        Mockito.when(identityService.obtainClientCredentials(tokenParametersFor(CORRELATION_ISSUER_CONNECTOR)))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(TOKEN_VALUE).build()));
    }

    @Test
    public void testConnectorIdInvalidUrn() {
        String urn = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONTRACT_OFFER.getValue(), CONNECTOR_ID);

        Assertions.assertThrows(EdcException.class, () -> new IdsResponseMessageFactory(urn, identityService));
    }

    @Test
    public void testConnectorIdValidUrn() {
        String urn = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), CONNECTOR_ID);

        Assertions.assertDoesNotThrow(() -> new IdsResponseMessageFactory(urn, identityService));
    }

    @Test
    public void testConnectorIdNoUrn() {
        Assertions.assertDoesNotThrow(() -> new IdsResponseMessageFactory(CONNECTOR_ID, identityService));
    }

    @Test
    public void testMessageId() {

        Consumer<Message> assertFunc = (message) -> {
            IdsId messageId = IdsIdParser.parse(message.getId().toString());
            Assertions.assertNotNull(messageId);
            Assertions.assertEquals(IdsType.MESSAGE, messageId.getType());
        };

        Assertions.assertAll("message id",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }


    @Test
    public void testSenderAgent() {
        String expectedSenderAgent = String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, IdsType.CONNECTOR.getValue(), CONNECTOR_ID);

        Consumer<Message> assertFunc = (message) -> Assertions.assertEquals(expectedSenderAgent, message.getSenderAgent().toString());

        Assertions.assertAll("sender agent",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testCorrelationMessageId() {
        Consumer<Message> assertFunc = (message) -> Assertions.assertEquals(CORRELATION_MESSAGE_ID, message.getCorrelationMessage().toString());

        Assertions.assertAll("correlation message id",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Disabled // TODO enable with IDS-Serializer from issue 236
    @Test
    public void testIssued() {
        Consumer<Message> assertFunc = (message) -> {
            boolean isOlderThanNowMinus5Sec = Instant.now().getEpochSecond() - 5 < message.getIssued().toGregorianCalendar().toZonedDateTime().toEpochSecond();
            boolean isNewerThanNowPlus5Sec = Instant.now().getEpochSecond() + 5 > message.getIssued().toGregorianCalendar().toZonedDateTime().toEpochSecond();

            Assertions.assertTrue(isOlderThanNowMinus5Sec && isNewerThanNowPlus5Sec);
        };

        Assertions.assertAll("issued",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testSecurityTokenValue() {
        Consumer<Message> assertFunc = (message) -> Assertions.assertEquals(TOKEN_VALUE, message.getSecurityToken().getTokenValue());

        Assertions.assertAll("security token value",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }


    @Test
    public void testSecurityTokenFormat() {
        Consumer<Message> assertFunc = (message) -> Assertions.assertEquals(TokenFormat.JWT, message.getSecurityToken().getTokenFormat());

        Assertions.assertAll("security token format",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }


    @Test
    public void testModelVersion() {
        Consumer<Message> assertFunc = (message) -> Assertions.assertEquals(IdsProtocol.INFORMATION_MODEL_VERSION, message.getModelVersion());

        Assertions.assertAll("model version",
                () -> assertFunc.accept(factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertFunc.accept(factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testCorrelationMessageNull() {
        Mockito.when(correlationMessage.getId()).thenReturn(null);

        Consumer<Provider<Message>> assertFunc = (provider) -> Assertions.assertThrows(InvalidCorrelationMessageException.class, provider::get);

        // rejection message for exceptions uses a placeholder instead of throwing an exception
        Consumer<Provider<Message>> assertExFunc = (provider) -> {
            Message message = provider.get();
            Assertions.assertEquals(IdsResponseMessageFactory.NULL_CORRELATION_MESSAGE_ID, message.getCorrelationMessage());
        };

        Assertions.assertAll("correlation message null",
                () -> assertFunc.accept(() -> factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertExFunc.accept(() -> factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testSenderAgentNull() {
        Mockito.when(correlationMessage.getSenderAgent()).thenReturn(null);

        Consumer<Provider<Message>> assertFunc = (provider) -> Assertions.assertThrows(InvalidCorrelationMessageException.class, provider::get);

        // rejection message for exceptions uses a placeholder instead of throwing an exception
        Consumer<Provider<Message>> assertExFunc = (provider) -> {
            Message message = provider.get();
            Assertions.assertEquals(IdsResponseMessageFactory.NULL_RECIPIENT_AGENT, message.getRecipientAgent().get(0));
        };

        Assertions.assertAll("sender agent null",
                () -> assertFunc.accept(() -> factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertExFunc.accept(() -> factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testIssuerConnectorNull() {
        Mockito.when(correlationMessage.getIssuerConnector()).thenReturn(null);
        Mockito.when(identityService.obtainClientCredentials(tokenParametersFor(IdsResponseMessageFactory.NULL_RECIPIENT_CONNECTOR.toString())))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(TOKEN_VALUE).build()));

        Consumer<Provider<Message>> assertFunc = (provider) -> Assertions.assertThrows(InvalidCorrelationMessageException.class, provider::get);

        // rejection message for exceptions uses a placeholder instead of throwing an exception
        Consumer<Provider<Message>> assertExFunc = (provider) -> {
            Message message = provider.get();
            Assertions.assertEquals(IdsResponseMessageFactory.NULL_RECIPIENT_CONNECTOR, message.getRecipientConnector().get(0));
        };

        Assertions.assertAll("issuer connector null",
                () -> assertFunc.accept(() -> factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertExFunc.accept(() -> factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testClientCredentialsMissing() {
        Mockito.when(identityService.obtainClientCredentials(tokenParametersFor(CORRELATION_ISSUER_CONNECTOR)))
                .thenReturn(Result.failure("foo"));

        Consumer<Provider<Message>> assertFunc = (provider) -> Assertions.assertThrows(MissingClientCredentialsException.class, provider::get);

        // rejection message for exceptions uses a placeholder instead of throwing an exception
        Consumer<Provider<Message>> assertExFunc = (provider) -> {
            Message message = provider.get();
            Assertions.assertEquals(IdsResponseMessageFactory.NULL_TOKEN, message.getSecurityToken().getTokenValue());
        };

        Assertions.assertAll("client credentials missing",
                () -> assertFunc.accept(() -> factory.createRequestInProcessMessage(correlationMessage)),
                () -> assertExFunc.accept(() -> factory.createRejectionMessage(correlationMessage, exception))
        );
    }

    @Test
    public void testRejectionReasons() {

        BiConsumer<Exception, RejectionReason> assertFunc = (exception, reason) -> {
            RejectionMessage message = factory.createRejectionMessage(correlationMessage, exception);
            Assertions.assertEquals(reason, message.getRejectionReason());
        };

        Assertions.assertAll("rejection reasons",
                () -> assertFunc.accept(InvalidCorrelationMessageException.createExceptionForCorrelationIdMissing(),
                        RejectionReason.BAD_PARAMETERS),
                () -> assertFunc.accept(new MissingClientCredentialsException(Collections.emptyList()),
                        RejectionReason.INTERNAL_RECIPIENT_ERROR),
                () -> assertFunc.accept(Mockito.mock(Exception.class),
                        RejectionReason.INTERNAL_RECIPIENT_ERROR)
        );
    }

    private static TokenParameters tokenParametersFor(String audience) {
        return argThat(t -> t != null &&
                Objects.equals(t.getScope(), IdsClientCredentialsScope.ALL) &&
                Objects.equals(t.getAudience(), audience));
    }
}
