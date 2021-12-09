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
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractOfferToIdsContractOfferTransformerTest {
    private static final String CONTRACT_OFFER_ID = "456uz984390236s";
    private static final URI OFFER_ID = URI.create("urn:offer:" + CONTRACT_OFFER_ID);
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private ContractOfferToIdsContractOfferTransformer transformer;

    private Policy policy;
    private ContractOffer contractOffer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractOfferToIdsContractOfferTransformer();
        contractOffer = mock(ContractOffer.class);
        policy = mock(Policy.class);
        context = mock(TransformerContext.class);

        when(contractOffer.getPolicy()).thenReturn(policy);
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
            transformer.transform(contractOffer, null);
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

        when(contractOffer.getId()).thenReturn(CONTRACT_OFFER_ID);
        when(contractOffer.getProvider()).thenReturn(PROVIDER_URI);
        when(contractOffer.getConsumer()).thenReturn(null);
        when(contractOffer.getContractStart()).thenReturn(null);
        when(contractOffer.getContractEnd()).thenReturn(null);
        when(policy.getPermissions()).thenReturn(Collections.singletonList(edcPermission));
        when(policy.getProhibitions()).thenReturn(Collections.singletonList(edcProhibition));
        when(policy.getObligations()).thenReturn(Collections.singletonList(edcObligation));

        when(context.transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class))).thenReturn(idsPermission);
        when(context.transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class))).thenReturn(idsProhibition);
        when(context.transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsObligation);
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(OFFER_ID);

        var result = transformer.transform(contractOffer, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(OFFER_ID, result.getId());
        Assertions.assertEquals(PROVIDER_URI, result.getProvider());
        Assertions.assertEquals(1, result.getObligation().size());
        Assertions.assertEquals(idsObligation, result.getObligation().get(0));
        Assertions.assertEquals(1, result.getPermission().size());
        Assertions.assertEquals(idsPermission, result.getPermission().get(0));
        Assertions.assertEquals(1, result.getProhibition().size());
        Assertions.assertEquals(idsProhibition, result.getProhibition().get(0));
    }
}
