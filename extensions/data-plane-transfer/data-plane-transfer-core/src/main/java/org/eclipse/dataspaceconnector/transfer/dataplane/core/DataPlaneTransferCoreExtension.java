/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core;

import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.transfer.dataplane.core.security.NoopDataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

@Provides({DataEncrypter.class})
public class DataPlaneTransferCoreExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Data Plane Transfer Core";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(DataEncrypter.class, new NoopDataEncrypter());
    }
}
