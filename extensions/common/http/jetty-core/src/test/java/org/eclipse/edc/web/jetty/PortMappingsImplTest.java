/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.web.jetty;

import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortMappingsImplTest {

    private final PortMappingsImpl portMappings = new PortMappingsImpl();

    @Test
    void shouldReturnNoMappings_whenNoRegistration() {
        assertThat(portMappings.getAll()).isEmpty();
    }

    @Test
    void shouldReturnRegisteredMappings() {
        var mapping = new PortMapping("name", 9292, "/path");
        portMappings.register(mapping);

        var result = portMappings.getAll();

        assertThat(result).hasSize(1).containsOnly(mapping);
    }

    @Test
    void shouldThrowException_whenMappingForPortAlreadyExist() {
        var mapping = new PortMapping("name", 9292, "/path");
        portMappings.register(mapping);

        var invalidMapping = new PortMapping("invalid", 9292, "/invalid");

        assertThatThrownBy(() -> portMappings.register(invalidMapping)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowException_whenPathDoesNotStartWithSlash() {
        var mapping = new PortMapping("name", 9292, "without/trailing/slash");

        assertThatThrownBy(() -> portMappings.register(mapping)).isInstanceOf(IllegalArgumentException.class);
    }

}
