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

import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Supplies context data for CEL expression evaluation specific to TransferProcessPolicyContext.
 */
public class TransferProcessContextMapper implements CelContextMapper<TransferProcessPolicyContext> {

    private final AgreementContextMapper agreementContextSupplier;
    private final ParticipantAgentContextMapper<TransferProcessPolicyContext> participantAgentContextSupplier;

    public TransferProcessContextMapper(AgreementContextMapper agreementContextSupplier, ParticipantAgentContextMapper<TransferProcessPolicyContext> participantAgentContextSupplier) {
        this.agreementContextSupplier = agreementContextSupplier;
        this.participantAgentContextSupplier = participantAgentContextSupplier;
    }

    @Override
    public Result<Map<String, Object>> mapContext(TransferProcessPolicyContext context) {
        return agreementContextSupplier.mapContext(context)
                .compose(ctx -> appendContext(participantAgentContextSupplier, context, ctx));
    }

    private Result<Map<String, Object>> appendContext(CelContextMapper<TransferProcessPolicyContext> supplier, TransferProcessPolicyContext ctx, Map<String, Object> toAppend) {
        return supplier.mapContext(ctx).map(params -> {
            var map = new HashMap<>(params);
            map.putAll(toAppend);
            return map;
        });
    }
}
