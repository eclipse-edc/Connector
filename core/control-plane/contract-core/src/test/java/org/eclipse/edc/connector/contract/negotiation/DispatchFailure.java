/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.negotiation;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.UnaryOperator;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;

public class DispatchFailure implements Arguments {

    private final ContractNegotiationStates starting;
    private final ContractNegotiationStates ending;
    private final UnaryOperator<ContractNegotiation.Builder> builderEnricher;

    public DispatchFailure() {
        this(INITIAL, INITIAL, it -> it);
    }

    public DispatchFailure(ContractNegotiationStates starting, ContractNegotiationStates ending, UnaryOperator<ContractNegotiation.Builder> builderEnricher) {
        this.starting = starting;
        this.ending = ending;
        this.builderEnricher = builderEnricher;
    }

    @Override
    public Object[] get() {
        return new Object[]{ starting, ending, builderEnricher };
    }
}
