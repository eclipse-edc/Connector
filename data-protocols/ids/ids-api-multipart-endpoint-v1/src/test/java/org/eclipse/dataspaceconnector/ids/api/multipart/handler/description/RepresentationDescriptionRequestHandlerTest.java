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
import de.fraunhofer.iais.eis.Representation;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockAssetIndex;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepresentationDescriptionRequestHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    private RepresentationDescriptionRequestHandler representationDescriptionRequestHandler;

    private Monitor monitor;
    private IdsTransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private AssetIndex assetIndex;
    private Representation representation;

    @BeforeEach
    public void setup() throws URISyntaxException {
        monitor = mock(Monitor.class);
        representation = mock(Representation.class);
        when(representation.getId()).thenReturn(new URI("urn:representation:hello"));

        assetIndex = mockAssetIndex();
        when(assetIndex.findById(anyString())).thenReturn(Asset.Builder.newInstance().build());

        transformerRegistry = mockTransformerRegistry(IdsType.REPRESENTATION);
        when(transformerRegistry.transform(isA(Asset.class), eq(Representation.class))).thenReturn(Result.success(representation));

        descriptionRequestMessage = mockDescriptionRequestMessage(representation.getId());

        representationDescriptionRequestHandler = new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, transformerRegistry);
    }

    @Test
    public void testConstructorArgumentsNotNullable() {
        assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(null, CONNECTOR_ID, assetIndex, transformerRegistry));
        assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, null, assetIndex, transformerRegistry));
        assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, null, transformerRegistry));
        assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        var verificationResult = Result.success(ClaimToken.Builder.newInstance().build());
        var result = representationDescriptionRequestHandler.handle(descriptionRequestMessage, verificationResult, null);

        assertNotNull(result);
        assertNotNull(result.getHeader());
        assertEquals(representation, result.getPayload());
        verify(representation).getId();
        verify(assetIndex).findById(anyString());
        verify(transformerRegistry).transform(isA(Asset.class), eq(Representation.class));
    }
}
