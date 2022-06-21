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
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class DescriptionRequestHandlerMocks {

    public static AssetIndex mockAssetIndex() {
        AssetIndex assetIndex = mock(AssetIndex.class);
        Asset asset = Asset.Builder.newInstance().id("urn:asset:123456").build();
        when(assetIndex.findById(isA(String.class))).thenReturn(asset);
        return assetIndex;
    }

    public static IdsTransformerRegistry mockTransformerRegistry(IdsType type) throws URISyntaxException {
        IdsTransformerRegistry transformerRegistry = mock(IdsTransformerRegistry.class);
        when(transformerRegistry.transform(isA(IdsId.class), eq(URI.class)))
                .thenReturn(Result.success(new URI("https://example.com")));
        when(transformerRegistry.transform(isA(URI.class), eq(IdsId.class)))
                .thenReturn(Result.success(IdsId.Builder.newInstance().type(type).value("value").build()));
        return transformerRegistry;
    }

    public static DescriptionRequestMessage mockDescriptionRequestMessage(URI requestedElement) throws URISyntaxException {
        DescriptionRequestMessage descriptionRequestMessage = mock(DescriptionRequestMessage.class);
        when(descriptionRequestMessage.getId()).thenReturn(new URI("https://correlation-id.com/"));
        when(descriptionRequestMessage.getSenderAgent()).thenReturn(new URI("https://sender-agent.com/"));
        when(descriptionRequestMessage.getIssuerConnector()).thenReturn(new URI("https://issuer-connector.com/"));
        when(descriptionRequestMessage.getRequestedElement()).thenReturn(requestedElement);
        return descriptionRequestMessage;
    }
}
