/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.transferprocess;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.context.ProtocolRequestContext;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.model.Policy;

public record RequestTransferContext(ContractAgreement contractAgreement) implements ProtocolRequestContext {
    @Override
    public RequestPolicyContext.Provider requestPolicyContextProvider() {
        return RequestTransferProcessPolicyContext::new;
    }

    @Override
    public Policy policy() {
        return contractAgreement.getPolicy();
    }
}
