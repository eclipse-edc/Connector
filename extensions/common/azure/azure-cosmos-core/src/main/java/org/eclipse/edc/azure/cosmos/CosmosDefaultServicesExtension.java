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

package org.eclipse.edc.azure.cosmos;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Provides default service implementations for fallback
 */

@Extension(value = CosmosDefaultServicesExtension.NAME)
public class CosmosDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CosmosClientProvider cosmosClientProvider() {
        return new CosmosClientProviderImpl();
    }

}
