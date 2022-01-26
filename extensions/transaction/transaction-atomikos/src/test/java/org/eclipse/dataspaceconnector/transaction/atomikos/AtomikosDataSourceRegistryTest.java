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
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfiguration.DataSourceType.NON_XA;

class AtomikosDataSourceRegistryTest {

    @Test
    void verifyInitialization() {
        var registry = new AtomikosDataSourceRegistry();
        registry.initialize(DataSourceConfiguration.Builder.newInstance().name("default").driverClass("com.Driver").url("jdbc://foo").build());
        registry.initialize(DataSourceConfiguration.Builder.newInstance().name("non-xa").dataSourceType(NON_XA).driverClass("com.Driver").url("jdbc://foo").build());
        assertThat(registry.resolve("default")).isNotNull();
    }
}
