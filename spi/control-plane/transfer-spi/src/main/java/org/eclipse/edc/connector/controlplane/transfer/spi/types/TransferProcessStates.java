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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the states a consumer and provider transfer process can be in.
 */
public enum TransferProcessStates {
    INITIAL(100),
    @Deprecated(since = "0.16.0")
    PROVISIONING(200),
    PROVISIONING_REQUESTED(250),
    @Deprecated(since = "0.16.0")
    PROVISIONED(300),
    REQUESTING(400),
    REQUESTED(500),
    STARTING(550),
    STARTUP_REQUESTED(570),
    STARTED(600),
    SUSPENDING(650),
    SUSPENDING_REQUESTED(675),
    SUSPENDED(700),
    RESUMING(720),
    RESUMED(725),
    COMPLETING(750),
    COMPLETING_REQUESTED(775),
    COMPLETED(800),
    TERMINATING(825),
    TERMINATING_REQUESTED(840),
    TERMINATED(850),
    @Deprecated(since = "0.16.0")
    DEPROVISIONING(900),
    @Deprecated(since = "0.16.0")
    DEPROVISIONING_REQUESTED(950),
    @Deprecated(since = "0.16.0")
    DEPROVISIONED(1000);

    private final int code;
    private static final List<Integer> FINAL_STATES = List.of(COMPLETED.code(), TERMINATED.code(), DEPROVISIONED.code());

    TransferProcessStates(int code) {
        this.code = code;
    }

    public static TransferProcessStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public static boolean isFinal(int state) {
        return FINAL_STATES.contains(state);
    }

    public int code() {
        return code;
    }
}
