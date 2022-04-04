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
package org.eclipse.dataspaceconnector.transfer.provision.http.config;

import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.PROVISIONER_CONFIG;
import static org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerFixtures.TEST_DATA_TYPE;
import static org.eclipse.dataspaceconnector.transfer.provision.http.config.ConfigParser.parseConfigurations;

class ConfigParserTest {

    @Test
    void verifyParse() {
        var config = ConfigFactory.fromMap(PROVISIONER_CONFIG);
        var configurations = parseConfigurations(config);
        assertThat(configurations.size()).isEqualTo(1);

        var configuration = configurations.get(0);

        assertThat(configuration.getProvisionerType()).isEqualTo(ProvisionerConfiguration.ProvisionerType.PROVIDER);
        assertThat(configuration.getPolicyScope()).isEqualTo("provision1.scope");
        assertThat(configuration.getEndpoint().toString()).isEqualTo("http://foo.com");
        assertThat(configuration.getDataAddressType()).isEqualTo(TEST_DATA_TYPE);
    }


}
