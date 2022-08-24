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

package org.eclipse.dataspaceconnector.azure.cosmos;

import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

public class CosmosClientProviderExtension implements ServiceExtension {

    @Override
    public String name() {
        return "CosmosDB Client Provider";
    }

    @Provider(isDefault = true)
    public CosmosClientProvider cosmosClientProvider() {
        return new CosmosClientProviderImpl();
    }

}
