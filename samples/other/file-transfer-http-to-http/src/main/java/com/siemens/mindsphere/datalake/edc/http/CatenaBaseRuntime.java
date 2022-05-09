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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package com.siemens.mindsphere.datalake.edc.http;

import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;

public class CatenaBaseRuntime extends BaseRuntime {

    /**
     * The {@code main} method must be re-implemented, otherwise {@link BaseRuntime#main(String[])} would be called, which would
     * instantiate the {@code BaseRuntime}.
     */
    public static void main(String[] args) {
        new CatenaBaseRuntime().boot();
    }
}
