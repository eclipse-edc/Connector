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

import org.easymock.EasyMock;
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

public class ContractAgreementToIdsContractAgreementTransformerTest {
    private static final URI AGREEMENT_ID = URI.create("urn:agreement:456uz984390236s");
    private static final String PROVIDER_ID = "https://provider.com/";

    // subject
    private ContractAgreementToIdsContractAgreementTransformer transformer;

    // mocks
    private Policy policy;
    private ContractAgreement contractAgreement;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ContractAgreementToIdsContractAgreementTransformer();
        contractAgreement = EasyMock.createMock(ContractAgreement.class);
        policy = EasyMock.createMock(Policy.class);
        context = EasyMock.createMock(TransformerContext.class);

        EasyMock.expect(contractAgreement.getPolicy()).andReturn(policy);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(contractAgreement, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(contractAgreement, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(contractAgreement, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(contractAgreement, context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare

        Permission edcPermission = EasyMock.createMock(Permission.class);
        de.fraunhofer.iais.eis.Permission idsPermission = EasyMock.createMock(de.fraunhofer.iais.eis.Permission.class);
        Prohibition edcProhibition = EasyMock.createMock(Prohibition.class);
        de.fraunhofer.iais.eis.Prohibition idsProhibition = EasyMock.createMock(de.fraunhofer.iais.eis.Prohibition.class);
        Duty edcObligation = EasyMock.createMock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsObligation = EasyMock.createMock(de.fraunhofer.iais.eis.Duty.class);

        EasyMock.expect(contractAgreement.getProviderAgentId()).andReturn(PROVIDER_ID);
        EasyMock.expect(contractAgreement.getConsumerAgentId()).andReturn(null);
        EasyMock.expect(contractAgreement.getContractStartDate()).andReturn(Instant.MIN.getEpochSecond());
        EasyMock.expect(contractAgreement.getContractEndDate()).andReturn(Instant.MAX.getEpochSecond());
        EasyMock.expect(contractAgreement.getContractSigningDate()).andReturn(Instant.now().getEpochSecond());
        EasyMock.expect(policy.getPermissions()).andReturn(Collections.singletonList(edcPermission)).times(2);
        EasyMock.expect(policy.getProhibitions()).andReturn(Collections.singletonList(edcProhibition)).times(2);
        EasyMock.expect(policy.getObligations()).andReturn(Collections.singletonList(edcObligation)).times(2);

        EasyMock.expect(context.transform(EasyMock.anyObject(Permission.class), EasyMock.eq(de.fraunhofer.iais.eis.Permission.class))).andReturn(idsPermission);
        EasyMock.expect(context.transform(EasyMock.anyObject(Prohibition.class), EasyMock.eq(de.fraunhofer.iais.eis.Prohibition.class))).andReturn(idsProhibition);
        EasyMock.expect(context.transform(EasyMock.anyObject(Duty.class), EasyMock.eq(de.fraunhofer.iais.eis.Duty.class))).andReturn(idsObligation);
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(AGREEMENT_ID);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(contractAgreement, policy, context);

        // invoke
        var result = transformer.transform(contractAgreement, context);

        // verify
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
