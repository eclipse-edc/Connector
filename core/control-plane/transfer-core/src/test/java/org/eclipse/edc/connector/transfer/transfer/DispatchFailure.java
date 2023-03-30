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

package org.eclipse.edc.connector.transfer.transfer;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.UnaryOperator;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;


public class DispatchFailure implements Arguments {

    private final TransferProcessStates starting;
    private final TransferProcessStates ending;
    private final UnaryOperator<TransferProcess.Builder> builderEnricher;

    public DispatchFailure() {
        this(INITIAL, INITIAL, it -> it);
    }

    public DispatchFailure(TransferProcessStates starting, TransferProcessStates ending, UnaryOperator<TransferProcess.Builder> builderEnricher) {
        this.starting = starting;
        this.ending = ending;
        this.builderEnricher = builderEnricher;
    }

    @Override
    public Object[] get() {
        return new Object[]{ starting, ending, builderEnricher };
    }
}
