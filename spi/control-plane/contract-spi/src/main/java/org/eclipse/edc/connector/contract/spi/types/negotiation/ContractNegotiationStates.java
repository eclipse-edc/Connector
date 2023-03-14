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
 *       Fraunhofer Institute for Software and Systems Engineering - minor modifications
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import java.util.Arrays;

/**
 * Defines the states a contract negotiation can be in.
 */
public enum ContractNegotiationStates {

    INITIAL(50),
    CONSUMER_REQUESTING(100),
    CONSUMER_REQUESTED(200),
    PROVIDER_OFFERING(300),
    PROVIDER_OFFERED(400),
    CONSUMER_AGREEING(700),
    CONSUMER_AGREED(800),
    PROVIDER_AGREEING(825),
    PROVIDER_AGREED(850),
    DECLINING(900),
    DECLINED(1000),
    CONSUMER_VERIFYING(1050),
    CONSUMER_VERIFIED(1100),
    PROVIDER_FINALIZING(1150),
    PROVIDER_FINALIZED(1200),
    TERMINATING(1300),
    TERMINATED(1400);

    private final int code;

    ContractNegotiationStates(int code) {
        this.code = code;
    }

    public static ContractNegotiationStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }

}
