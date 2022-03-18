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

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractOfferToIdsContractOfferTransformerTest {
    private static final String CONTRACT_OFFER_ID = "456uz984390236s";
    private static final URI OFFER_ID = URI.create("urn:offer:" + CONTRACT_OFFER_ID);
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private ContractOfferToIdsContractOfferTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractOfferToIdsContractOfferTransformer();
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
            transformer.transform(contractOffer(Policy.Builder.newInstance().build()), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
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
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(OFFER_ID);

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
        verify(context).transform(isA(IdsId.class), eq(URI.class));
    }

    @NotNull
    private ContractOffer contractOffer(Policy policy) {
        return ContractOffer.Builder.newInstance()
                .id(CONTRACT_OFFER_ID)
                .policy(policy)
                .provider(PROVIDER_URI)
                .build();
    }
}
