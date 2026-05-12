/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils;

import org.eclipse.edc.junit.utils.LazySupplier;

import java.net.URI;
import java.util.Map;

public record Participants(Participant provider, Participant consumer) {

    /**
     * Default DSP profile id used in tests. Matches the bundled default profile registered by
     * {@code DspApiConfigurationV2025Extension}. Tests that need a different profile id should
     * use the 6-arg {@link Participant} constructor and ensure the participant config sets
     * {@code edc.dsp.profiles} accordingly.
     */
    public static final String DEFAULT_PROFILE_ID = "http-dsp-profile-2025-1";


    public record Participant(String contextId, String id, LazySupplier<URI> protocol,
                              LazySupplier<URI> signalingProtocol,
                              Map<String, String> config,
                              String profile) {

        public Participant(String contextId, String id, LazySupplier<URI> protocol, LazySupplier<URI> signalingProtocol) {
            this(contextId, id, protocol, signalingProtocol, Map.of(), DEFAULT_PROFILE_ID);
        }

        public Participant(String contextId, String id, LazySupplier<URI> protocol, LazySupplier<URI> signalingProtocol, Map<String, String> config) {
            this(contextId, id, protocol, signalingProtocol, config, DEFAULT_PROFILE_ID);
        }

        /**
         * Builds the protocol endpoint URL: {@code <protocol>/<contextId>/<profileId>/<dspVersion>}.
         * The {@code profileId} segment selects the dataspace profile (JSON-LD context, namespace,
         * identity mechanism); the {@code dspVersion} segment selects which controller class
         * dispatches the request.
         */
        public String getProtocolEndpoint() {
            return protocol.get() + "/" + contextId + "/" + profile;
        }

        public String getSignalingEndpointUrl() {
            return signalingProtocol.get().toString();
        }
    }
}
