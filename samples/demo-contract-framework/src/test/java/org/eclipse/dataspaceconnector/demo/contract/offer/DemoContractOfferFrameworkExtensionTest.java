/*
 *  Copyright (c) 2021 BMW Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       BMW Group - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.contract.offer;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
public class DemoContractOfferFrameworkExtensionTest {

    private static final String HTTP_PORT = "9999";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ConfigurationExtension.class, testConfiguration());
        extension.registerServiceMock(PolicyEngine.class, EasyMock.createMock(PolicyEngine.class));
    }

    @Test
    void testBoot(ContractOfferFramework framework) {
        assertThat(framework).isNotNull();
    }

    @NotNull
    private ConfigurationExtension testConfiguration() {
        return key -> "web.http.port".equals(key) ? HTTP_PORT : null;
    }
}
