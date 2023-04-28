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

package org.eclipse.edc.vault.filesystem;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

public final class FsConfiguration {

    @Setting(value = "The location of the Vault file, that contains sensitive data")
    static final String VAULT_LOCATION = "edc.vault";

    @Setting(value = "The path to the Keystore")
    static final String KEYSTORE_LOCATION = "edc.keystore";

    @Setting(value = "The password for the Keystore")
    static final String KEYSTORE_PASSWORD = "edc.keystore.password";

    @Setting(value = " Boolean flag that determines whether the vault file should be persistent or not. When set to true, the vault file will be stored and will not get lost when the system restarts")
    static final String PERSISTENT_VAULT = "edc.vault.persistent";


    private FsConfiguration() {
    }
}
