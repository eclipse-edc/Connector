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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandlerSettings;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ArtifactDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ConnectorDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DataCatalogDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.RepresentationDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.ResourceDescriptionRequestHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescriptionHandlerTest {

    // subject
    private DescriptionHandler descriptionHandler;

    // mocks
    private Monitor monitor;
    private DescriptionHandlerSettings descriptionHandlerSettings;
    private TransformerRegistry transformerRegistry;
    private ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler;
    private DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler;
    private RepresentationDescriptionRequestHandler representationDescriptionRequestHandler;
    private ResourceDescriptionRequestHandler resourceDescriptionRequestHandler;
    private ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;

    @BeforeEach
    void setUp() {
        monitor = EasyMock.mock(Monitor.class);
        descriptionHandlerSettings = EasyMock.mock(DescriptionHandlerSettings.class);
        transformerRegistry = EasyMock.mock(TransformerRegistry.class);
        artifactDescriptionRequestHandler = EasyMock.mock(ArtifactDescriptionRequestHandler.class);
        dataCatalogDescriptionRequestHandler = EasyMock.mock(DataCatalogDescriptionRequestHandler.class);
        representationDescriptionRequestHandler = EasyMock.mock(RepresentationDescriptionRequestHandler.class);
        resourceDescriptionRequestHandler = EasyMock.mock(ResourceDescriptionRequestHandler.class);
        connectorDescriptionRequestHandler = EasyMock.mock(ConnectorDescriptionRequestHandler.class);

        descriptionHandler = new DescriptionHandler(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);
    }

    @Test
    void testCanHandleNullThrowsNullPointerException() {
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);

        assertThrows(NullPointerException.class, () -> {
            descriptionHandler.canHandle(null);
        });
    }

    @Test
    void testCanHandleMultipartHeaderOfTypeDescriptionRequestMessageReturnsTrue() {
        DescriptionRequestMessage message = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                message);

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
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader,
                responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageWithUnknownRequestedElementErrorResponse() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(URI.create("urn:test:abc"));
        EasyMock.expect(requestHeader.getId()).andReturn(null);
        EasyMock.expect(requestHeader.getSenderAgent()).andReturn(null);
        EasyMock.expect(requestHeader.getIssuerConnector()).andReturn(null);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(URI.class), EasyMock.eq(IdsType.class)))
                .andReturn(new TransformResult<>(Collections.singletonList("unknown type")));

        EasyMock.expect(descriptionHandlerSettings.getId()).andReturn(null);

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isInstanceOf(RejectionMessage.class);
    }


    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageUnknownButValidRequestedElement() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(URI.create("urn:abc:1"));
        EasyMock.expect(requestHeader.getId()).andReturn(null);
        EasyMock.expect(requestHeader.getSenderAgent()).andReturn(null);
        EasyMock.expect(requestHeader.getIssuerConnector()).andReturn(null);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        EasyMock.expect(descriptionHandlerSettings.getId()).andReturn(null);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(URI.class), EasyMock.eq(IdsType.class)))
                .andReturn(new TransformResult<>(IdsType.PARTICIPANT));

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull()
                .extracting(MultipartResponse::getHeader).isInstanceOf(RejectionMessage.class);
        assertThat(((RejectionMessage) result.getHeader()).getRejectionReason())
                .isEqualTo(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageForArtifact() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        Message responseHeader = EasyMock.mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        URI uri = URI.create("urn:artifact:1");
        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(uri);

        EasyMock.expect(transformerRegistry.transform(uri, IdsType.class))
                .andReturn(new TransformResult<>(IdsType.ARTIFACT));

        EasyMock.expect(artifactDescriptionRequestHandler.handle(EasyMock.eq(requestHeader), EasyMock.anyObject())).andReturn(response);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader,
                responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageForCatalog() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        Message responseHeader = EasyMock.mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        URI uri = URI.create("urn:catalog:1");
        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(uri);

        EasyMock.expect(transformerRegistry.transform(uri, IdsType.class))
                .andReturn(new TransformResult<>(IdsType.CATALOG));

        EasyMock.expect(dataCatalogDescriptionRequestHandler.handle(EasyMock.eq(requestHeader), EasyMock.anyObject())).andReturn(response);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader,
                responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageForRepresentation() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        Message responseHeader = EasyMock.mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        URI uri = URI.create("urn:representation:1");
        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(uri);

        EasyMock.expect(transformerRegistry.transform(uri, IdsType.class))
                .andReturn(new TransformResult<>(IdsType.REPRESENTATION));

        EasyMock.expect(representationDescriptionRequestHandler.handle(EasyMock.eq(requestHeader), EasyMock.anyObject())).andReturn(response);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader,
                responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessageForResource() {
        // prepare
        DescriptionRequestMessage requestHeader = EasyMock.mock(DescriptionRequestMessage.class);

        Message responseHeader = EasyMock.mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        URI uri = URI.create("urn:resource:1");
        EasyMock.expect(requestHeader.getRequestedElement()).andReturn(uri);

        EasyMock.expect(transformerRegistry.transform(uri, IdsType.class))
                .andReturn(new TransformResult<>(IdsType.RESOURCE));

        EasyMock.expect(resourceDescriptionRequestHandler.handle(EasyMock.eq(requestHeader), EasyMock.anyObject())).andReturn(response);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();

        // record
        EasyMock.replay(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler,
                requestHeader,
                responseHeader);

        // invoke
        var result = descriptionHandler.handleRequest(multipartRequest);

        // verify
        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(
                monitor,
                descriptionHandlerSettings,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler
        );
    }
}