/*
 *  Copyright (c) 2025 Cofinity-X
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

package org.eclipse.edc.connector.controlplane.profile;

import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataspaceProfileContextRegistryImplTest {

    private final DataspaceProfileContextRegistry registry = new DataspaceProfileContextRegistryImpl();

    @Nested
    class GetAllVersions {
        @Test
        void shouldReturnVersions_whenContextsRegisteredDefault() {
            var version = new ProtocolVersion("version name", "/path");
            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url"));

            var result = registry.getProtocolVersions().protocolVersions();

            assertThat(result).hasSize(1).containsExactly(version);
        }

        @Test
        void shouldIgnoreDefaultContexts_whenStandardAreRegistered() {
            var defaultVersion = new ProtocolVersion("default", "/path");
            var standardVersion = new ProtocolVersion("default", "/path");
            registry.registerDefault(new DataspaceProfileContext("default", defaultVersion, () -> "url"));
            registry.register(new DataspaceProfileContext("standard", standardVersion, () -> "url"));

            var result = registry.getProtocolVersions().protocolVersions();

            assertThat(result).hasSize(1).containsExactly(standardVersion);
        }
    }

    @Nested
    class Resolve {

        @Test
        void shouldReturnNull_whenNoWebhookFound() {
            var result = registry.getWebhook("unexistent");

            assertThat(result).isNull();
        }

        @Test
        void shouldReturnWebhookForName() {
            var version = new ProtocolVersion("version name", "/path");
            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url"));

            var result = registry.getWebhook("profile");

            assertThat(result.url()).isEqualTo("url");
        }
    }

    @Nested
    class GetVersion {

        @Test
        void shouldReturnNull_whenNoVersionFound() {
            var result = registry.getProtocolVersion("unexistent");

            assertThat(result).isNull();
        }

        @Test
        void shouldReturnVersionForName() {
            var version = new ProtocolVersion("version name", "/path");
            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url"));

            var result = registry.getProtocolVersion("profile");

            assertThat(result).isEqualTo(version);
        }
    }
}
