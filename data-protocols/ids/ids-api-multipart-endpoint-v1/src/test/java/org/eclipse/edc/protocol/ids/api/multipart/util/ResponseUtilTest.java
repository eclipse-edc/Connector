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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.util;

import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.ResponseMessageBuilder;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseUtilTest {
    private final IdsId connectorId = IdsId.from("urn:connector:38bfeade-3566-11ec-8d3d-0242ac130003").getContent();
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

    @Test
    void createMultipartResponse_onlyHeader() {
        var header = new ResponseMessageBuilder().build();

        var multipartResponse = ResponseUtil.createMultipartResponse(header);

        assertThat(multipartResponse).isNotNull();
        assertThat(multipartResponse.getHeader()).isNotNull().isEqualTo(header);
        assertThat(multipartResponse.getPayload()).isNull();
    }

    @Test
    void createMultipartResponse_headerAndPayload() {
        var header = new ResponseMessageBuilder().build();
        var payload = "payload";

        var multipartResponse = ResponseUtil.createMultipartResponse(header, payload);

        assertThat(multipartResponse).isNotNull();
        assertThat(multipartResponse.getHeader()).isNotNull().isEqualTo(header);
        assertThat(multipartResponse.getPayload()).isNotNull().isEqualTo(payload);
    }

    @Test
    void testMessageProcessedNotification() {
        var message = ResponseUtil.messageProcessedNotification(correlationMessage, connectorId);

        assertBasePropertiesMapped(message, null);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testRequestInProcess() {
        var message = ResponseUtil.requestInProcess(correlationMessage, connectorId);

        assertBasePropertiesMapped(message, null);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testDescriptionResponse() {
        var message = ResponseUtil.descriptionResponse(correlationMessage, connectorId);

        assertBasePropertiesMapped(message, null);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testInProcessFromStatusResult_succeeded() {
        var result = StatusResult.success();

        var message = ResponseUtil.inProcessFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(RequestInProcessMessage.class);
        assertBasePropertiesMapped(message, null);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testInProcessFromStatusResult_fatalError() {
        var result = StatusResult.failure(ResponseStatus.FATAL_ERROR);

        var message = ResponseUtil.inProcessFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(RejectionMessage.class);
        assertBasePropertiesMapped(message, RejectionReason.BAD_PARAMETERS);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testInProcessFromStatusResult_errorRetry() {
        var result = StatusResult.failure(ResponseStatus.ERROR_RETRY);

        var message = ResponseUtil.inProcessFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(RejectionMessage.class);
        assertBasePropertiesMapped(message, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(message);
    }

    @Test
    void testProcessedFromStatusResult_succeeded() {
        var result = StatusResult.success();

        var message = ResponseUtil.processedFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(MessageProcessedNotificationMessage.class);
        assertBasePropertiesMapped(message, null);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testProcessedFromStatusResult_fatalError() {
        var result = StatusResult.failure(ResponseStatus.FATAL_ERROR);

        var message = ResponseUtil.processedFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(RejectionMessage.class);
        assertBasePropertiesMapped(message, RejectionReason.BAD_PARAMETERS);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testProcessedFromStatusResult_errorRetry() {
        var result = StatusResult.failure(ResponseStatus.ERROR_RETRY);

        var message = ResponseUtil.processedFromStatusResult(result, correlationMessage, connectorId);

        assertThat(message).isInstanceOf(RejectionMessage.class);
        assertBasePropertiesMapped(message, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(message);
        assertConnectorIdPropertiesMapped(message);
    }

    @Test
    void testNotFound() {
        var rejectionMessage = ResponseUtil.notFound(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_FOUND);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    void testNotAuthenticated() {
        var rejectionMessage = ResponseUtil.notAuthenticated(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHENTICATED);
        assertConnectorIdPropertiesMapped(rejectionMessage);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
    }

    @Test
    void testNotAuthorized() {
        var rejectionMessage = ResponseUtil.notAuthorized(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.NOT_AUTHORIZED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    void testMalformedMessage() {
        var rejectionMessage = ResponseUtil.malformedMessage(null, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertConnectorIdPropertiesMapped(rejectionMessage);

        rejectionMessage = ResponseUtil.malformedMessage(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MALFORMED_MESSAGE);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    void testMessageTypeNotSupported() {
        var rejectionMessage = ResponseUtil.messageTypeNotSupported(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    void testBadParameters() {
        var rejectionMessage = ResponseUtil.badParameters(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.BAD_PARAMETERS);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    @Test
    void testInternalRecipientError() {
        var rejectionMessage = ResponseUtil.internalRecipientError(correlationMessage, connectorId);

        assertBasePropertiesMapped(rejectionMessage, RejectionReason.INTERNAL_RECIPIENT_ERROR);
        assertCorrelationMessagePropertiesMapped(rejectionMessage);
        assertConnectorIdPropertiesMapped(rejectionMessage);
    }

    private void assertBasePropertiesMapped(Message message, RejectionReason rejectionReason) {
        if (message instanceof RejectionMessage) {
            assertThat((RejectionMessage) message)
                    .isNotNull()
                    .extracting(RejectionMessage::getRejectionReason)
                    .isEqualTo(rejectionReason);
        }

        assertThat(message.getContentVersion()).isEqualTo(IdsConstants.INFORMATION_MODEL_VERSION);
        assertThat(message.getModelVersion()).isEqualTo(IdsConstants.INFORMATION_MODEL_VERSION);
        assertThat(message.getIssued()).isNotNull();
    }

    private void assertCorrelationMessagePropertiesMapped(Message message) {
        assertThat(message).isNotNull();

        assertThat(message.getCorrelationMessage()).isEqualTo(correlationMessageId);

        assertThat(message.getRecipientAgent()).isNotNull();
        assertThat(message.getRecipientAgent()).hasSize(1);
        assertThat(message.getRecipientAgent().contains(senderAgent)).isTrue();

        assertThat(message.getRecipientConnector()).isNotNull();
        assertThat(message.getRecipientConnector()).hasSize(1);
        assertThat(message.getRecipientConnector().contains(issuerConnector)).isTrue();
    }

    private void assertConnectorIdPropertiesMapped(Message message) {
        assertThat(message).isNotNull();

        assertThat(message.getIssuerConnector()).isEqualTo(connectorId.toUri());
        assertThat(message.getSenderAgent()).isEqualTo(connectorId.toUri());
    }
}
