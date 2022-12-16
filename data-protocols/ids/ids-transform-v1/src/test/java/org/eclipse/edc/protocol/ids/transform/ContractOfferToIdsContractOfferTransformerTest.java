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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.transform.type.contract.ContractOfferToIdsContractOfferTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractOfferToIdsContractOfferTransformerTest {
    private static final String CONTRACT_OFFER_ID = "456uz984390236s";
    private static final URI OFFER_ID = URI.create("urn:contractoffer:" + CONTRACT_OFFER_ID);
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private ContractOfferToIdsContractOfferTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractOfferToIdsContractOfferTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulSimple() {
        Permission edcPermission = mock(Permission.class);
        de.fraunhofer.iais.eis.Permission idsPermission = mock(de.fraunhofer.iais.eis.Permission.class);
        Prohibition edcProhibition = mock(Prohibition.class);
        de.fraunhofer.iais.eis.Prohibition idsProhibition = mock(de.fraunhofer.iais.eis.Prohibition.class);
        Duty edcObligation = mock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsObligation = mock(de.fraunhofer.iais.eis.Duty.class);

        var policy = Policy.Builder.newInstance()
                .permission(edcPermission)
                .prohibition(edcProhibition)
                .duty(edcObligation)
                .build();
        var contractOffer = contractOffer(policy);

        when(context.transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class))).thenReturn(idsPermission);
        when(context.transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class))).thenReturn(idsProhibition);
        when(context.transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsObligation);

        var result = transformer.transform(contractOffer, context);

        assertNotNull(result);
        assertEquals(OFFER_ID, result.getId());
        assertEquals(PROVIDER_URI, result.getProvider());
        assertEquals(1, result.getObligation().size());
        assertEquals(idsObligation, result.getObligation().get(0));
        assertEquals(1, result.getPermission().size());
        assertEquals(idsPermission, result.getPermission().get(0));
        assertEquals(1, result.getProhibition().size());
        assertEquals(idsProhibition, result.getProhibition().get(0));

        verify(context).transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class));
        verify(context).transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class));
        verify(context).transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class));
    }

    @NotNull
    private ContractOffer contractOffer(Policy policy) {
        return ContractOffer.Builder.newInstance()
                .id(CONTRACT_OFFER_ID)
                .policy(policy)
                .asset(Asset.Builder.newInstance().id("test-asset").build())
                .provider(PROVIDER_URI)
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(1))
                .build();
    }
}
