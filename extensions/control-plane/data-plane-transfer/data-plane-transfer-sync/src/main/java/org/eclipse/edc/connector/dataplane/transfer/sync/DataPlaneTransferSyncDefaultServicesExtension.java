/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.sync;

import org.eclipse.edc.connector.dataplane.transfer.spi.security.DataEncrypter;
import org.eclipse.edc.connector.dataplane.transfer.sync.security.NoopDataEncrypter;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension since this module already contains {@link DataPlaneTransferSyncExtension}}
 */
public class DataPlaneTransferSyncDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Sync Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataEncrypter getDataEncrypter(ServiceExtensionContext context) {
        context.getMonitor().warning("No DataEncrypter registered, a no-op implementation will be used, not suitable for production environments");
        return new NoopDataEncrypter();
    }
}
