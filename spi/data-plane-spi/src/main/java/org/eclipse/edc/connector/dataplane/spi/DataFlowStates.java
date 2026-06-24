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

package org.eclipse.edc.connector.dataplane.spi;

import java.util.Arrays;

/**
 * Defines data flow states.
 */
public enum DataFlowStates {

    PROVISIONING(25),
    PROVISION_REQUESTED(40),
    PROVISION_NOTIFYING(45),
    PROVISIONED(50),
    RECEIVED(100),
    STARTED(150),
    COMPLETED(200),
    SUSPENDED(225),
    TERMINATED(250),
    FAILED(300),
    NOTIFIED(400),
    DEPROVISIONING(500),
    DEPROVISION_REQUESTED(550),
    DEPROVISIONED(600),
    DEPROVISION_FAILED(700);

    private final int code;

    DataFlowStates(int code) {
        this.code = code;
    }

    public static DataFlowStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
