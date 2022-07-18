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

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResponseUtilTest {
    private final String connectorId = "38bfeade-3566-11ec-8d3d-0242ac130003";
    private final URI connectorIdUri = URI.create("urn:connector:" + connectorId);
    private final URI correlationMessageId = URI.create("urn:message:7c35205e-3566-11ec-8d3d-0242ac130003");
    private final URI senderAgent = URI.create("urn:sender:7c352356-3566-11ec-8d3d-0242ac130003");
    private final URI issuerConnector = URI.create("urn:issuer:7c35255e-3566-11ec-8d3d-0242ac130003");

    private Message correlationMessage;

    @BeforeEach
    void setUp() {
        correlationMessage = mock(Message.class);

        when(correlationMessage.getId()).thenReturn(correlationMessageId);
        when(correlationMessage.getSenderAgent()).thenReturn(senderAgent);
        when(correlationMessage.getIssuerConnector()).thenReturn(issuerConnector);
    }

    @AfterEach
    void tearDown() {
        verify(correlationMessage, atLeastOnce()).getId();
        verify(correlationMessage, atLeastOnce()).getSenderAgent();
        verify(correlationMessage, atLeastOnce()).getIssuerConnector();
    }

    @Test
    public void testNotFound() {
        var rejectionMessage = ResponseUtil
                .notFound(null, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);

        // just correlationMessage, no connectorId
        rejectionMessage = ResponseUtil
                .notFound(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        // no correlationMessage, just connectorId
        rejectionMessage = ResponseUtil
                .notFound(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        // both correlationMessage and connectorId
        rejectionMessage = ResponseUtil
                .notFound(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testNotAuthenticated() {
        var rejectionMessage = ResponseUtil
                .notAuthenticated(null, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);

        rejectionMessage = ResponseUtil
                .notAuthenticated(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .notAuthenticated(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .notAuthenticated(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertConnectorIdPropertiesMapped(rejectionMessage);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
    }

    @Test
    public void testNotAuthorized() {
        var rejectionMessage = ResponseUtil
                .notAuthorized(null, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);

        rejectionMessage = ResponseUtil
                .notAuthorized(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .notAuthorized(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .notAuthorized(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testMalformedMessage() {
        var rejectionMessage = ResponseUtil
                .malformedMessage(null, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);

        rejectionMessage = ResponseUtil
                .malformedMessage(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .malformedMessage(null, connectorId);


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .malformedMessage(correlationMessage, connectorId);


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testMessageTypeNotSupported() {
        var rejectionMessage = ResponseUtil
                .messageTypeNotSupported(null, null);


        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);

        rejectionMessage = ResponseUtil
                .messageTypeNotSupported(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .messageTypeNotSupported(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .messageTypeNotSupported(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    public void testInternalRecipientError() {
        var rejectionMessage = ResponseUtil
                .internalRecipientError(null, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);

        rejectionMessage = ResponseUtil
                .internalRecipientError(correlationMessage, null);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .internalRecipientError(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil
                .internalRecipientError(correlationMessage, connectorId);

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
}
