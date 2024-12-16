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

package org.eclipse.edc.web.jetty;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record JettyConfiguration(
        @Setting(key = "edc.web.https.keystore.password", description = "Keystore password", defaultValue = "password")
        String keystorePassword,
        @Setting(key = "edc.web.https.keymanager.password", description = "Keymanager password", defaultValue = "password")
        String keymanagerPassword
) {
}
