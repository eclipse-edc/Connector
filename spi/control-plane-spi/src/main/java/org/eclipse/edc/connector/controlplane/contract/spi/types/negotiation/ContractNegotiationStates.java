/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - minor modifications
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the states a contract negotiation can be in.
 */
public enum ContractNegotiationStates {

    INITIAL(50),
    REQUESTING(100),
    REQUESTED(200),
    OFFERING(300),
    OFFERED(400),
    ACCEPTING(700),
    ACCEPTED(800),
    AGREEING(825),
    AGREED(850),
    VERIFYING(1050),
    VERIFIED(1100),
    FINALIZING(1150),
    FINALIZED(1200),
    TERMINATING(1300),
    TERMINATED(1400);

    private final int code;
    private static final List<Integer> FINAL_STATES = List.of(FINALIZED.code(), TERMINATED.code());

    ContractNegotiationStates(int code) {
        this.code = code;
    }

    public static ContractNegotiationStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public static boolean isFinal(int state) {
        return FINAL_STATES.contains(state);
    }

    public int code() {
        return code;
    }

}
