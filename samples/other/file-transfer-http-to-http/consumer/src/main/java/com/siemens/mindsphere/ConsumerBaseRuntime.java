/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *
 */

package com.siemens.mindsphere;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;

/**
 * Download happens only from UI and there is a JWT token there that can be passed through (no need to use the technical user)
 */
public class ConsumerBaseRuntime extends BaseRuntime {

    /**
     * The {@code main} method must be re-implemented, otherwise {@link BaseRuntime#main(String[])} would be called, which would
     * instantiate the {@code BaseRuntime}.
     */
    public static void main(String[] args) {
        new ConsumerBaseRuntime().boot();
    }
}
