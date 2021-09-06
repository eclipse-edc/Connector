/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.policy;

/**
 * IDS policy context for consumer-side offer evaluation functions.
 */
public class IdsOfferPolicyContext {
    private final String processId;
    private final String providerConnectorId;

    public IdsOfferPolicyContext(String providerConnectorId, String processId) {
        this.providerConnectorId = providerConnectorId;
        this.processId = processId;
    }

    public String getProviderConnectorId() {
        return providerConnectorId;
    }

    public String getProcessId() {
        return processId;
    }
}
