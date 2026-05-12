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
 *       Cofinity-X - add participantId to DataspaceProfileContext
 *
 */

package org.eclipse.edc.connector.controlplane.profile;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataspaceProfileContextRegistryImplTest {

    private static final JsonLdNamespace NAMESPACE = new JsonLdNamespace("https://example.org/dspace/");
    private static final String CONTEXT_URL = "https://example.org/context.jsonld";

    private final DataspaceProfileContextRegistry registry = new DataspaceProfileContextRegistryImpl();

    @Nested
    class GetAllVersions {
        @Test
        void shouldReturnVersions_whenContextsRegisteredDefault() {
            var version = new ProtocolVersion("version name", "/path", "binding");
            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            var result = registry.getProtocolVersions().protocolVersions();

            assertThat(result).hasSize(1).containsExactly(version);
        }

        @Test
        void shouldIgnoreDefaultContexts_whenStandardAreRegistered() {
            var defaultVersion = new ProtocolVersion("default", "/path", "binding");
            var standardVersion = new ProtocolVersion("default", "/path", "binding");
            registry.registerDefault(new DataspaceProfileContext("default", defaultVersion, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));
            registry.register(new DataspaceProfileContext("standard", standardVersion, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            var result = registry.getProtocolVersions().protocolVersions();

            assertThat(result).hasSize(1).containsExactly(standardVersion);
        }
    }

    @Nested
    class GetProfiles {
        @Test
        void shouldReturnProfiles_whenContextsRegisteredDefault() {
            var version = new ProtocolVersion("version name", "/path", "binding");
            var profile = new DataspaceProfileContext("profile", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL));
            registry.registerDefault(profile);

            assertThat(registry.getProfiles()).hasSize(1).containsExactly(profile);
        }

        @Test
        void shouldIgnoreDefaultContexts_whenStandardAreRegistered() {
            var defaultVersion = new ProtocolVersion("default", "/path", "binding");
            var standardVersion = new ProtocolVersion("default", "/path", "binding");
            var defaultProfile = new DataspaceProfileContext("default", defaultVersion, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL));
            var standardProfile = new DataspaceProfileContext("standard", standardVersion, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL));

            registry.registerDefault(defaultProfile);
            registry.register(standardProfile);

            assertThat(registry.getProfiles()).hasSize(1).containsExactly(standardProfile);
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
            var version = new ProtocolVersion("version name", "/path", "binding");
            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            var result = registry.getProtocolVersion("profile");

            assertThat(result).isEqualTo(version);
        }
    }

    @Nested
    class GetProfileByProtocol {
        @Test
        void resolvesByBareId() {
            var version = new ProtocolVersion("v", "/v", "https");
            var profile = new DataspaceProfileContext("2025-1", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL));
            registry.registerDefault(profile);

            assertThat(registry.getProfile("2025-1")).isEqualTo(profile);
        }

        @Test
        void resolvesByBindingPrefixedProtocol() {
            var version = new ProtocolVersion("v", "/v", "https");
            var profile = new DataspaceProfileContext("2025-1", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL));
            registry.registerDefault(profile);

            assertThat(registry.getProfile("2025-1")).isEqualTo(profile);
        }

        @Test
        void returnsNullWhenNoMatch() {
            assertThat(registry.getProfile("dataspace-protocol-http:unknown")).isNull();
        }
    }

    @Nested
    class RegistrationCallback {
        @Test
        void firesOnSubsequentRegister() {
            var seen = new ArrayList<String>();
            registry.addRegistrationCallback(p -> seen.add(p.name()));
            var version = new ProtocolVersion("v", "/v", "https");

            registry.registerDefault(new DataspaceProfileContext("a", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));
            registry.register(new DataspaceProfileContext("b", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            assertThat(seen).containsExactly("a", "b");
        }

        @Test
        void replaysAlreadyRegisteredProfiles() {
            var version = new ProtocolVersion("v", "/v", "https");
            registry.registerDefault(new DataspaceProfileContext("a", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));
            registry.register(new DataspaceProfileContext("b", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            var seen = new ArrayList<String>();
            registry.addRegistrationCallback(p -> seen.add(p.name()));

            assertThat(seen).containsExactly("a", "b");
        }

        @Test
        void multipleCallbacks_eachFiresPerProfile() {
            var seenByOne = new ArrayList<String>();
            var seenByTwo = new ArrayList<String>();
            registry.addRegistrationCallback(p -> seenByOne.add(p.name()));
            registry.addRegistrationCallback(p -> seenByTwo.add(p.name()));
            var version = new ProtocolVersion("v", "/v", "https");

            registry.registerDefault(new DataspaceProfileContext("a", version, () -> "url", ct -> "id", NAMESPACE, List.of(CONTEXT_URL)));

            assertThat(seenByOne).containsExactly("a");
            assertThat(seenByTwo).containsExactly("a");
        }
    }

    @Nested
    class GetIdExtractionFunction {
        @Test
        void shouldReturnNull_whenNoIdExtractionFunctionFound() {
            var result = registry.getIdExtractionFunction("unexistent");

            assertThat(result).isNull();
        }

        @Test
        void shouldReturnIdExtractionFunctionForName() {
            var claimToken = ClaimToken.Builder.newInstance().build();
            var participantId = "participantId";
            var version = new ProtocolVersion("version name", "/path", "binding");

            registry.registerDefault(new DataspaceProfileContext("profile", version, () -> "url", ct -> participantId, NAMESPACE, List.of(CONTEXT_URL)));

            var result = registry.getIdExtractionFunction("profile");

            assertThat(result).isNotNull();
            assertThat(result.apply(claimToken)).isEqualTo(participantId);
        }
    }
}
