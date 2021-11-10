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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockAssetIndex;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;

public class RepresentationDescriptionRequestHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    // subject
    private RepresentationDescriptionRequestHandler representationDescriptionRequestHandler;

    // mocks
    private Monitor monitor;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private AssetIndex assetIndex;
    private Representation representation;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() throws URISyntaxException {
        monitor = EasyMock.createMock(Monitor.class);
        representation = EasyMock.createMock(Representation.class);
        EasyMock.expect(representation.getId()).andReturn(new URI("urn:representation:hello"));
        EasyMock.replay(monitor, representation);

        assetIndex = mockAssetIndex();
        EasyMock.expect(assetIndex.findById(EasyMock.anyString())).andReturn(EasyMock.createMock(Asset.class));
        EasyMock.replay(assetIndex);

        transformerRegistry = mockTransformerRegistry(IdsType.REPRESENTATION);
        var representationResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(representationResult.getOutput()).andReturn(representation);
        EasyMock.expect(representationResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(Asset.class), EasyMock.eq(Representation.class))).andReturn(representationResult);
        EasyMock.replay(transformerRegistry, representationResult);

        descriptionRequestMessage = mockDescriptionRequestMessage(representation.getId());
        EasyMock.replay(descriptionRequestMessage);

        representationDescriptionRequestHandler = new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, transformerRegistry);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(null, CONNECTOR_ID, assetIndex, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, null, assetIndex, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, null, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new RepresentationDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        VerificationResult verificationResult = EasyMock.createMock(VerificationResult.class);
        var result = representationDescriptionRequestHandler.handle(descriptionRequestMessage, verificationResult, null);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHeader());
        Assertions.assertEquals(representation, result.getPayload());
    }
}
