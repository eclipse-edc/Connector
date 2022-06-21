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
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferedAssetToIdsResourceTransformerTest {

    private static final String RESOURCE_ID = "test_id";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    private OfferedAssetToIdsResourceTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new OfferedAssetToIdsResourceTransformer();
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
            transformer.transform(assetAndPolicy(), null);
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
        var id = IdsId.Builder.newInstance().value(RESOURCE_ID).type(IdsType.RESOURCE).build();
        when(context.transform(any(Asset.class), eq(Representation.class))).thenReturn(representation);
        when(context.transform(any(ContractOffer.class), eq(de.fraunhofer.iais.eis.ContractOffer.class))).thenReturn(new ContractOfferBuilder().build());
        when(context.transform(eq(id), eq(URI.class))).thenReturn(RESOURCE_ID_URI);

        var result = transformer.transform(assetAndPolicy(), context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(RESOURCE_ID_URI, result.getId());
        Assertions.assertEquals(1, result.getRepresentation().size());
        Assertions.assertEquals(representation, result.getRepresentation().get(0));
        verify(context).transform(any(Asset.class), eq(Representation.class));
        verify(context).transform(any(ContractOffer.class), eq(de.fraunhofer.iais.eis.ContractOffer.class));
        verify(context).transform(eq(id), eq(URI.class));
    }

    @NotNull
    private OfferedAsset assetAndPolicy() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id("id")
                .policy(Policy.Builder.newInstance().build())
                .build();
        return new OfferedAsset(Asset.Builder.newInstance().id(RESOURCE_ID).build(), Collections.singletonList(contractOffer));
    }

}
