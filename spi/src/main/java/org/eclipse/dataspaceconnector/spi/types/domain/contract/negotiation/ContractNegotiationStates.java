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
 *
 */
package org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation;

import java.util.Arrays;

/**
 * Defines the states a contract negotiation can be in.
 */
public enum ContractNegotiationStates {

    UNSAVED(0),
    REQUESTING(100),
    REQUESTED(200),
    PROVIDER_OFFERING(300),
    PROVIDER_OFFERED(400),
    CLIENT_OFFERING(500),
    CLIENT_OFFERED(600),
    CLIENT_APPROVING(700),
    CLIENT_APPROVED(800),
    DECLINING(900),
    DECLINED(1000),
    CONFIRMING(1100),
    CONFIRMED(1200),
    ERROR(-1);

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
