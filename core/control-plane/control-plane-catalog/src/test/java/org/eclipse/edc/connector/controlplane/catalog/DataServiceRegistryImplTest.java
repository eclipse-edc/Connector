/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataServiceRegistryImplTest {

    private final DataServiceRegistryImpl registry = new DataServiceRegistryImpl();

    @Test
    void shouldReturnRegisteredDataService() {
        var dataService = DataService.Builder.newInstance().build();

        var protocol = "protocol";
        var participantContextId = "participantContextId";

        registry.register(protocol, (ctx, proto) -> dataService);
        var dataServices = registry.getDataServices(participantContextId, protocol);

        assertThat(dataServices).containsExactly(dataService);
    }

    @Test
    void shouldReturnEmptyDataServices() {
        var dataService = DataService.Builder.newInstance().build();

        var protocol = "protocol";
        var participantContextId = "participantContextId";

        registry.register(protocol, (ctx, proto) -> dataService);
        var dataServices = registry.getDataServices(participantContextId, "unknownProtocol");

        assertThat(dataServices).isEmpty();
    }

}
