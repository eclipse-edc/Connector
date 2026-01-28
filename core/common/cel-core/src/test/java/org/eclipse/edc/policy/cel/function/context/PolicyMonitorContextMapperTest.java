/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function.context;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyMonitorContextMapperTest {

    private final PolicyMonitorContextMapper mapper = new PolicyMonitorContextMapper(
            new AgreementContextMapper()
    );

    @SuppressWarnings("unchecked")
    @Test
    void mapContext() {
        var ctx = mock(PolicyMonitorContext.class);
        var agreement = ContractAgreement.Builder.newInstance()
                .providerId("provider-id")
                .consumerId("consumer-id")
                .id("id")
                .assetId("asset-id")
                .agreementId("agreement-id")
                .policy(Policy.Builder.newInstance().build())
                .build();

        when(ctx.contractAgreement()).thenReturn(agreement);
        var result = mapper.mapContext(ctx);

        assertThat(result).isSucceeded().satisfies(map -> {
            var contract = (Map<String, Object>) map.get("agreement");
            Assertions.assertThat(contract.get("id")).isEqualTo("id");
            Assertions.assertThat(contract.get("assetId")).isEqualTo("asset-id");
            Assertions.assertThat(contract.get("agreementId")).isEqualTo("agreement-id");
            Assertions.assertThat(contract.get("providerId")).isEqualTo("provider-id");
            Assertions.assertThat(contract.get("consumerId")).isEqualTo("consumer-id");

        });

    }
}
