/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.version;

import org.eclipse.dataspaceconnector.ids.spi.version.ConnectorVersionProvider;

// TODO move to edc-core
public class ConnectorVersionProviderImpl implements ConnectorVersionProvider {
    private static final String VERSION = "0.0.1";

    @Override
    public String getVersion() {
        return VERSION;
    }
}
