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

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;

public final class FsConfiguration {

    @Setting
    static final String VAULT_LOCATION = "edc.vault";

    @Setting
    static final String KEYSTORE_LOCATION = "edc.keystore";

    @Setting
    static final String KEYSTORE_PASSWORD = "edc.keystore.password";

    @Setting
    static final String PERSISTENT_VAULT = "edc.vault.persistent";


    private FsConfiguration() {
    }
}
