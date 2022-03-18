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
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.DescriptionHandler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DescriptionHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    private DescriptionHandler descriptionHandler;

    private Monitor monitor;
    private IdsTransformerRegistry transformerRegistry;
    private ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler;
    private DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler;
    private RepresentationDescriptionRequestHandler representationDescriptionRequestHandler;
    private ResourceDescriptionRequestHandler resourceDescriptionRequestHandler;
    private ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;

    @BeforeEach
    void setUp() {
        monitor = mock(Monitor.class);
        transformerRegistry = mock(IdsTransformerRegistry.class);
        artifactDescriptionRequestHandler = mock(ArtifactDescriptionRequestHandler.class);
        dataCatalogDescriptionRequestHandler = mock(DataCatalogDescriptionRequestHandler.class);
        representationDescriptionRequestHandler = mock(RepresentationDescriptionRequestHandler.class);
        resourceDescriptionRequestHandler = mock(ResourceDescriptionRequestHandler.class);
        connectorDescriptionRequestHandler = mock(ConnectorDescriptionRequestHandler.class);

        descriptionHandler = new DescriptionHandler(
                monitor,
                CONNECTOR_ID,
                transformerRegistry,
                artifactDescriptionRequestHandler,
                dataCatalogDescriptionRequestHandler,
                representationDescriptionRequestHandler,
                resourceDescriptionRequestHandler,
                connectorDescriptionRequestHandler);
    }

    @Test
    void testCanHandleNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            descriptionHandler.canHandle(null);
        });
    }

    @Test
    void testCanHandleMultipartHeaderOfTypeDescriptionRequestMessageReturnsTrue() {
        DescriptionRequestMessage message = mock(DescriptionRequestMessage.class);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(message)
                .build();

        var result = descriptionHandler.canHandle(multipartRequest);

        assertThat(result).isTrue();
    }

    @Test
    void testHandleRequestOfTypeDescriptionRequestMessage() {
        DescriptionRequestMessage requestHeader = mock(DescriptionRequestMessage.class);

        Message responseHeader = mock(Message.class);
        MultipartResponse response = MultipartResponse.Builder.newInstance()
                .header(responseHeader)
                .build();

        when(requestHeader.getRequestedElement()).thenReturn(null);

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(requestHeader)
                .build();
        var verificationResult = Result.success(ClaimToken.Builder.newInstance().build());

        when(connectorDescriptionRequestHandler.handle(requestHeader, verificationResult, multipartRequest.getPayload())).thenReturn(response);


        var result = descriptionHandler.handleRequest(multipartRequest, verificationResult);

        assertThat(result).isNotNull();
        assertThat(result).extracting(MultipartResponse::getHeader).isEqualTo(responseHeader);
        verify(connectorDescriptionRequestHandler).handle(requestHeader, verificationResult, multipartRequest.getPayload());
    }

}