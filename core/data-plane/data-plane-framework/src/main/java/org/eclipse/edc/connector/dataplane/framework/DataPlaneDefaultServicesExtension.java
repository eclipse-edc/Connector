/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.api.client.spi.transferprocess.NoopTransferProcessClient;
import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = DataPlaneDefaultServicesExtension.NAME)
public class DataPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Framework Default Services";

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public TransferProcessApiClient transferProcessApiClient() {
        return new NoopTransferProcessClient();
    }
}
