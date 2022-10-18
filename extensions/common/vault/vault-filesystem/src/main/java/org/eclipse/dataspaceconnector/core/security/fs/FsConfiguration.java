/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.core.security.fs;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;

public final class FsConfiguration {

    @EdcSetting
    static final String VAULT_LOCATION = "edc.vault";

    @EdcSetting
    static final String KEYSTORE_LOCATION = "edc.keystore";

    @EdcSetting
    static final String KEYSTORE_PASSWORD = "edc.keystore.password";

    @EdcSetting
    static final String PERSISTENT_VAULT = "edc.vault.persistent";


    private FsConfiguration() {
    }
}
