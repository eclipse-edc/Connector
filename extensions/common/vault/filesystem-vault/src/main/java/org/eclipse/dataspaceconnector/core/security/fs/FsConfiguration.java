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

import org.eclipse.dataspaceconnector.spi.EdcSetting;

import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

public final class FsConfiguration {

    @EdcSetting
    static final String VAULT_LOCATION = propOrEnv("edc.vault", "dataspaceconnector-vault.properties");

    @EdcSetting
    static final String KEYSTORE_LOCATION = propOrEnv("edc.keystore", "dataspaceconnector-keystore.jks");

    @EdcSetting
    static final String KEYSTORE_PASSWORD = propOrEnv("edc.keystore.password", null);

    @EdcSetting
    static final boolean PERSISTENT_VAULT = Boolean.parseBoolean(propOrEnv("edc.vault.persistent", "true"));


    private FsConfiguration() {
    }
}
