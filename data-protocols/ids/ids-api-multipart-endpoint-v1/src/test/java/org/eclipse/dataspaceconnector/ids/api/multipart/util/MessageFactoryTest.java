/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageFactoryTest {
    private final String connectorId = "38bfeade-3566-11ec-8d3d-0242ac130003";
    private final URI connectorIdUri = URI.create("urn:connector:" + connectorId);
    private final URI correlationMessageId = URI.create("urn:message:7c35205e-3566-11ec-8d3d-0242ac130003");
    private final URI senderAgent = URI.create("urn:sender:7c352356-3566-11ec-8d3d-0242ac130003");
    private final URI issuerConnector = URI.create("urn:issuer:7c35255e-3566-11ec-8d3d-0242ac130003");

    private Message correlationMessage;

    private MessageFactory messageFactory;

    @BeforeEach
    void setUp() {
        correlationMessage = mock(Message.class);
        IdentityService identityService = mock(IdentityService.class);
        messageFactory = new MessageFactory(connectorIdUri, identityService);

        when(correlationMessage.getId()).thenReturn(correlationMessageId);
        when(correlationMessage.getSenderAgent()).thenReturn(senderAgent);
        when(correlationMessage.getIssuerConnector()).thenReturn(issuerConnector);
        when(identityService.obtainClientCredentials("")).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c").build()));
    }

    @AfterEach
    void tearDown() {
        verify(correlationMessage, atLeastOnce()).getId();
        verify(correlationMessage, atLeastOnce()).getSenderAgent();
        verify(correlationMessage, atLeastOnce()).getIssuerConnector();
    }

    @Test
    public void testNotFound() {
        var rejectionMessage = messageFactory
                .rejectNotFound(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);

        // just correlationMessage, no connectorId
        rejectionMessage = messageFactory
                .rejectNotFound(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        // no correlationMessage, just connectorId
        rejectionMessage = messageFactory
                .rejectNotFound(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        // both correlationMessage and connectorId
        rejectionMessage = messageFactory
                .rejectNotFound(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testNotAuthenticated() {
        var rejectionMessage = messageFactory
                .rejectNotAuthenticated(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);

        rejectionMessage = messageFactory
                .rejectNotAuthenticated(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .rejectNotAuthenticated(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .rejectNotAuthenticated(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertConnectorIdPropertiesMapped(rejectionMessage);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
    }

    @Test
    public void testNotAuthorized() {
        var rejectionMessage = messageFactory
                .notAuthorized(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);

        rejectionMessage = messageFactory
                .notAuthorized(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .notAuthorized(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .notAuthorized(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testMalformedMessage() {
        var rejectionMessage = messageFactory
                .malformedMessage(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);

        rejectionMessage = messageFactory
                .malformedMessage(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .malformedMessage(createMessage());


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .malformedMessage(correlationMessage);


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testMessageTypeNotSupported() {
        var rejectionMessage = messageFactory
                .messageTypeNotSupported(createMessage());


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);

        rejectionMessage = messageFactory
                .messageTypeNotSupported(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .messageTypeNotSupported(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .messageTypeNotSupported(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testInternalRecipientError() {
        var rejectionMessage = messageFactory
                .internalRecipientError(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);

        rejectionMessage = messageFactory
                .internalRecipientError(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .internalRecipientError(createMessage());

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = messageFactory
                .internalRecipientError(correlationMessage);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    private void assertBasePropertiesMapped(RejectionMessage rejectionMessage, RejectionReason rejectionReason) {
        assertThat(rejectionMessage).isNotNull()
                .extracting(RejectionMessage::getRejectionReason).isEqualTo(rejectionReason);

        assertThat(rejectionMessage.getContentVersion()).isEqualTo(IdsProtocol.INFORMATION_MODEL_VERSION);
        assertThat(rejectionMessage.getModelVersion()).isEqualTo(IdsProtocol.INFORMATION_MODEL_VERSION);
        assertThat(rejectionMessage.getIssued()).isNotNull();
    }

    private void assertCorrelationMessagePropertiesMapped(RejectionMessage rejectionMessage) {
        assertThat(rejectionMessage).isNotNull();

        assertThat(rejectionMessage.getCorrelationMessage()).isEqualTo(correlationMessageId);

        assertThat(rejectionMessage.getRecipientAgent()).isNotNull();
        assertThat(rejectionMessage.getRecipientAgent()).hasSize(1);
        assertThat(rejectionMessage.getRecipientAgent().contains(senderAgent)).isTrue();

        assertThat(rejectionMessage.getRecipientConnector()).isNotNull();
        assertThat(rejectionMessage.getRecipientConnector()).hasSize(1);
        assertThat(rejectionMessage.getRecipientConnector().contains(issuerConnector)).isTrue();
    }

    private void assertConnectorIdPropertiesMapped(RejectionMessage rejectionMessage) {
        assertThat(rejectionMessage).isNotNull();

        assertThat(rejectionMessage.getIssuerConnector()).isEqualTo(connectorIdUri);
        assertThat(rejectionMessage.getSenderAgent()).isEqualTo(connectorIdUri);
    }

    private Message createMessage() {
        return new DescriptionRequestMessageBuilder()
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("xxxxx.yyyyy.zzzzz").build())
                ._issuerConnector_(URI.create("urn:connector:issuer"))
                ._senderAgent_(URI.create("urn:connector:sender"))
                ._recipientConnector_(Collections.singletonList(URI.create("urn:connector:recipient")))
                ._requestedElement_(URI.create("urn:artifact:1"))
                .build();
    }
}
