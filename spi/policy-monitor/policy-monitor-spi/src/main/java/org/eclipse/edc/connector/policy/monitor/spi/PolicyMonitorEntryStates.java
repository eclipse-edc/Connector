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

package org.eclipse.edc.connector.policy.monitor.spi;

import java.util.Arrays;

/**
 * States for the {@link PolicyMonitorEntry} entity.
 */
public enum PolicyMonitorEntryStates {
    STARTED(100),
    COMPLETED(200),
    FAILED(300);

    private final int code;

    PolicyMonitorEntryStates(int code) {
        this.code = code;
    }

    public static PolicyMonitorEntryStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
