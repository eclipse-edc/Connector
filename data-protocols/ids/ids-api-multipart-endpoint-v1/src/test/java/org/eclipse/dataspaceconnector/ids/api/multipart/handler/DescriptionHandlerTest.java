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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescriptionHandlerTest {

    // subject
    private DescriptionHandler descriptionHandler;

    //mocks
    private DescriptionHandlerSettings descriptionHandlerSettings;
    private ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;

    @BeforeEach
    void setUp() {
        descriptionHandlerSettings = EasyMock.mock(DescriptionHandlerSettings.class);
        connectorDescriptionRequestHandler = EasyMock.mock(ConnectorDescriptionRequestHandler.class);

        descriptionHandler = new DescriptionHandler(descriptionHandlerSettings, connectorDescriptionRequestHandler);
    }

    @Test
    void testCanHandleNullThrowsNullPointerException() {
        EasyMock.replay(descriptionHandlerSettings, connectorDescriptionRequestHandler);

        assertThrows(NullPointerException.class, () -> {
            descriptionHandler.canHandle(null);
        });
    }

    @Test
    void testCanHandleMultipartHeaderOfTypeDescriptionRequestMessageReturnsTrue() {
        DescriptionRequestMessage message = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.replay(descriptionHandlerSettings, connectorDescriptionRequestHandler, message);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(message)
                .build();

        var result = descriptionHandler.canHandle(multipartRequest);

        assertThat(result).isTrue();
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessage() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);
        String requestPayload = null;

        Message responseHeader = EasyMock.mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(null);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        EasyMock.expect(connectorDescriptionRequestHandler.handle(requestHeader, multipartRequest.getPayload())).andReturn(response);

        // record
        EasyMock.replay(descriptionHandlerSettings, connectorDescriptionRequestHandler, requestHeader, responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageWithUnknownRequestedElementSchemaThrows() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(URI.create("urn:test:abc"));

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        // record
        EasyMock.replay(descriptionHandlerSettings, connectorDescriptionRequestHandler, requestHeader);

        // invoke
        assertThrows(IllegalArgumentException.class, () -> {
            descriptionHandler.handleRequest(multipartRequest);
        });
    }


    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageUnknwonButValidRequestedElement() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(IdsId.participant("test").toUri());
        EasyMock.expect(requestHeader.getId()).andReturn(null);
        EasyMock.expect(requestHeader.getSenderAgent()).andReturn(null);
        EasyMock.expect(requestHeader.getIssuerConnector()).andReturn(null);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        EasyMock.expect(descriptionHandlerSettings.getId()).andReturn(null);

        // record
        EasyMock.replay(descriptionHandlerSettings, connectorDescriptionRequestHandler, requestHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull()
                .extracting(MultipartResponse::getHeader).isInstanceOf(RejectionMessage.class);
        assertThat(((RejectionMessage) result.getHeader()).getRejectionReason())
                .isEqualTo(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(descriptionHandlerSettings, connectorDescriptionRequestHandler);
    }
}