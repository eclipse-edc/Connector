/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.transfer.spi.types;

import java.util.Arrays;

/**
 * Defines the states a consumer and provider transfer process can be in.
 */
public enum TransferProcessStates {
    INITIAL(100),
    PROVISIONING(200),
    PROVISIONING_REQUESTED(250),
    PROVISIONED(300),
    REQUESTING(400),
    REQUESTED(500),
    STARTED(600),
    COMPLETED(800),
    DEPROVISIONING(900),
    DEPROVISIONING_REQUESTED(950),
    DEPROVISIONED(1000),
    TERMINATED(1100),
    CANCELLED(1200),
    ERROR(-1);

    private final int code;

    TransferProcessStates(int code) {
        this.code = code;
    }

    public static TransferProcessStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
