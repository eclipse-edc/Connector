/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractAgreementToIdsContractAgreementTransformerTest {
    private static final URI AGREEMENT_ID = URI.create("urn:agreement:456uz984390236s");
    private static final String PROVIDER_ID = "https://provider.com/";

    private ContractAgreementToIdsContractAgreementTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractAgreementToIdsContractAgreementTransformer();
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
            transformer.transform(contractAgreement(Policy.Builder.newInstance().build()), null);
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

        when(context.transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class))).thenReturn(idsPermission);
        when(context.transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class))).thenReturn(idsProhibition);
        when(context.transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsObligation);
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(AGREEMENT_ID);

        var result = transformer.transform(contractAgreement(policy), context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AGREEMENT_ID, result.getId());
        Assertions.assertEquals(PROVIDER_ID, String.valueOf(result.getProvider()));
        Assertions.assertEquals(1, result.getObligation().size());
        Assertions.assertEquals(idsObligation, result.getObligation().get(0));
        Assertions.assertEquals(1, result.getPermission().size());
        Assertions.assertEquals(idsPermission, result.getPermission().get(0));
        Assertions.assertEquals(1, result.getProhibition().size());
        Assertions.assertEquals(idsProhibition, result.getProhibition().get(0));
        verify(context).transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class));
        verify(context).transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class));
        verify(context).transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class));
        verify(context).transform(isA(IdsId.class), eq(URI.class));
    }

    private ContractAgreement contractAgreement(Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(String.valueOf(AGREEMENT_ID))
                .providerAgentId(PROVIDER_ID)
                .asset(Asset.Builder.newInstance().build())
                .consumerAgentId("id")
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(policy)
                .build();
    }
}
