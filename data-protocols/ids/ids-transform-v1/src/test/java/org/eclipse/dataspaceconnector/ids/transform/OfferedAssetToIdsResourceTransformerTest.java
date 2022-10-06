/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.ids.transform.type.asset.OfferedAssetToIdsResourceTransformer;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferedAssetToIdsResourceTransformerTest {

    private static final String RESOURCE_ID = "1";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    private final TransformerContext context = mock(TransformerContext.class);

    private OfferedAssetToIdsResourceTransformer transformer;

    @NotNull
    private static ContractOffer createContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("test-asset").build())
                .build();
    }

    @BeforeEach
    void setUp() {
        transformer = new OfferedAssetToIdsResourceTransformer();
    }

    @Test
    void transform_nullInputGivesNullResult() {
        var result = transformer.transform(null, context);

        assertThat(result).isNull();
    }

    @Test
    void transform() {
        var representation = new RepresentationBuilder().build();
        when(context.transform(any(Asset.class), eq(Representation.class))).thenReturn(representation);
        when(context.transform(any(ContractOffer.class), eq(de.fraunhofer.iais.eis.ContractOffer.class))).thenReturn(new ContractOfferBuilder().build());
        var offeredAsset = new OfferedAsset(
                Asset.Builder.newInstance().id(RESOURCE_ID).build(),
                List.of(createContractOffer(), createContractOffer())
        );

        var result = transformer.transform(offeredAsset, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(RESOURCE_ID_URI);
        assertThat(result.getRepresentation()).hasSize(1).first().isEqualTo(representation);
        assertThat(result.getContractOffer()).hasSize(2);
        verify(context).transform(any(Asset.class), eq(Representation.class));
        verify(context, times(2)).transform(any(ContractOffer.class), eq(de.fraunhofer.iais.eis.ContractOffer.class));
    }

}
