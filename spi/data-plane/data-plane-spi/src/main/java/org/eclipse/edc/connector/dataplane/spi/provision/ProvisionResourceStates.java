/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.provision;

import java.util.Arrays;

/**
 * Defines Provision resource definition states
 */
public enum ProvisionResourceStates {

    CREATED(100),
    PROVISION_REQUESTED(150),
    PROVISIONED(200),
    DEPROVISION_REQUESTED(400),
    DEPROVISIONED(500);

    private final int code;

    ProvisionResourceStates(int code) {
        this.code = code;
    }

    public static ProvisionResourceStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
