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
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContractAgreementToIdsContractAgreementTransformerTest {
    private static final URI AGREEMENT_ID = URI.create("urn:agreement:456uz984390236s");
    private static final String PROVIDER_ID = "https://provider.com/";

    private ContractAgreementToIdsContractAgreementTransformer transformer;

    private Policy policy;
    private ContractAgreement contractAgreement;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractAgreementToIdsContractAgreementTransformer();
        contractAgreement = mock(ContractAgreement.class);
        policy = mock(Policy.class);
        context = mock(TransformerContext.class);

        when(contractAgreement.getPolicy()).thenReturn(policy);
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
            transformer.transform(contractAgreement, null);
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

        when(contractAgreement.getId()).thenReturn(String.valueOf(AGREEMENT_ID));
        when(contractAgreement.getProviderAgentId()).thenReturn(PROVIDER_ID);
        when(contractAgreement.getConsumerAgentId()).thenReturn(null);
        when(contractAgreement.getContractStartDate()).thenReturn(Instant.MIN.getEpochSecond());
        when(contractAgreement.getContractEndDate()).thenReturn(Instant.MAX.getEpochSecond());
        when(contractAgreement.getContractSigningDate()).thenReturn(Instant.now().getEpochSecond());
        when(policy.getPermissions()).thenReturn(Collections.singletonList(edcPermission));
        when(policy.getProhibitions()).thenReturn(Collections.singletonList(edcProhibition));
        when(policy.getObligations()).thenReturn(Collections.singletonList(edcObligation));

        when(context.transform(any(Permission.class), eq(de.fraunhofer.iais.eis.Permission.class))).thenReturn(idsPermission);
        when(context.transform(any(Prohibition.class), eq(de.fraunhofer.iais.eis.Prohibition.class))).thenReturn(idsProhibition);
        when(context.transform(any(Duty.class), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsObligation);
        when(context.transform(isA(IdsId.class), eq(URI.class))).thenReturn(AGREEMENT_ID);

        var result = transformer.transform(contractAgreement, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AGREEMENT_ID, result.getId());
        Assertions.assertEquals(PROVIDER_ID, String.valueOf(result.getProvider()));
        Assertions.assertEquals(1, result.getObligation().size());
        Assertions.assertEquals(idsObligation, result.getObligation().get(0));
        Assertions.assertEquals(1, result.getPermission().size());
        Assertions.assertEquals(idsPermission, result.getPermission().get(0));
        Assertions.assertEquals(1, result.getProhibition().size());
        Assertions.assertEquals(idsProhibition, result.getProhibition().get(0));
    }
}
