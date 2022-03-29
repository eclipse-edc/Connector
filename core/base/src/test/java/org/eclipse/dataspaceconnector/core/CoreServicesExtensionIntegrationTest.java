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

package org.eclipse.dataspaceconnector.core;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.Hostname;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.core.CoreServicesExtension.HOSTNAME_SETTING;

@ExtendWith(EdcExtension.class)
class CoreServicesExtensionIntegrationTest {

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(HOSTNAME_SETTING, "hostname"));
    }

    @Test
    void shouldProvideHostnameExtension(Hostname hostname) {
        assertThat(hostname.get()).isEqualTo("hostname");
    }

}