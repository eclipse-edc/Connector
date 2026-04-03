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

    public record Participant(String contextId, String id, LazySupplier<URI> protocol,
                              LazySupplier<URI> signalingProtocol,
                              Map<String, String> config) {

        public Participant(String contextId, String id, LazySupplier<URI> protocol, LazySupplier<URI> signalingProtocol) {
            this(contextId, id, protocol, signalingProtocol, Map.of());
        }

        public String getProtocolEndpoint() {
            return protocol.get() + "/" + contextId + "/2025-1";
        }

        public String getSignalingEndpointUrl() {
            return signalingProtocol.get().toString();
        }
    }
}
