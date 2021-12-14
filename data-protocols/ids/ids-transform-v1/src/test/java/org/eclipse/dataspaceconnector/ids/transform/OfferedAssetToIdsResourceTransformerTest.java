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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferedAssetToIdsResourceTransformerTest {

    private static final String RESOURCE_ID = "test_id";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    private OfferedAssetToIdsResourceTransformer transformer;

    private ContractOffer contractOffer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new OfferedAssetToIdsResourceTransformer();
        contractOffer = mock(ContractOffer.class);
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(assetAndPolicy(contractOffer), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        var representation = new RepresentationBuilder().build();
        when(context.transform(any(Asset.class), eq(Representation.class))).thenReturn(representation);
        when(context.transform(any(ContractOffer.class), eq(de.fraunhofer.iais.eis.ContractOffer.class))).thenReturn(new ContractOfferBuilder().build());

        IdsId id = IdsId.Builder.newInstance().value(RESOURCE_ID).type(IdsType.RESOURCE).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(RESOURCE_ID_URI);

        var result = transformer.transform(assetAndPolicy(contractOffer), context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(RESOURCE_ID_URI, result.getId());
        Assertions.assertEquals(1, result.getRepresentation().size());
        Assertions.assertEquals(representation, result.getRepresentation().get(0));
    }

    @NotNull
    private OfferedAsset assetAndPolicy(ContractOffer contractOffer) {
        return new OfferedAsset(Asset.Builder.newInstance().id(RESOURCE_ID).build(), Collections.singletonList(contractOffer));
    }

}
