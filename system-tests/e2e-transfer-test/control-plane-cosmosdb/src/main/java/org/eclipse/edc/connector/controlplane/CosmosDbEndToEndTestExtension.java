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

package org.eclipse.edc.connector.controlplane;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.spi.system.ServiceExtension;

public class CosmosDbEndToEndTestExtension implements ServiceExtension {

    @Provider
    public CosmosClientProvider testClientProvider() {
        return (vault, configuration) -> CosmosTestClient.createClient();
    }
}
