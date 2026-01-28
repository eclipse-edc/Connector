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

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Supplies agreement-related context data for CEL expression evaluation.
 */
public class AgreementContextMapper implements CelContextMapper<AgreementPolicyContext> {

    @Override
    public Result<Map<String, Object>> mapContext(AgreementPolicyContext context) {
        var agreement = context.contractAgreement();
        return Result.success(Map.of("agreement", Map.ofEntries(
                Map.entry("id", agreement.getId()),
                Map.entry("assetId", agreement.getAssetId()),
                Map.entry("providerId", agreement.getProviderId()),
                Map.entry("consumerId", agreement.getConsumerId()),
                Map.entry("agreementId", agreement.getAgreementId()),
                Map.entry("contractSigningDate", agreement.getContractSigningDate())
        )));
    }
}
