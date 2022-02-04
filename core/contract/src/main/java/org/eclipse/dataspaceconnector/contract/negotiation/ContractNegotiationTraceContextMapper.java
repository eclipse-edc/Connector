package org.eclipse.dataspaceconnector.contract.negotiation;

/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - extended method implementation
 *
 */

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

public class ContractNegotiationTraceContextMapper implements TextMapGetter<ContractNegotiation>, TextMapSetter<ContractNegotiation.Builder> {

    public static ContractNegotiationTraceContextMapper INSTANCE = new ContractNegotiationTraceContextMapper();

    private ContractNegotiationTraceContextMapper() {
    }

    @Override
    public String get(ContractNegotiation carrier, String key) {
        return carrier.getTraceContext().get(key);
    }

    @Override
    public Iterable<String> keys(ContractNegotiation carrier) {
        return carrier.getTraceContext().keySet();
    }

    @Override
    public void set(ContractNegotiation.Builder carrier, String key, String value) {
        carrier.traceContext(key, value);
    }
}