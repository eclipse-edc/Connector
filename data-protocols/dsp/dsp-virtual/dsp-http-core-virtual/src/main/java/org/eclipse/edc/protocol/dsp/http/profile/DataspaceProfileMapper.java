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

package org.eclipse.edc.protocol.dsp.http.profile;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.ProtocolVersion;

/**
 * Converts a persisted {@link DataspaceProfile} into a live {@link DataspaceProfileContext}, resolving the
 * webhook address and default participant-id extraction function that cannot be persisted.
 */
class DataspaceProfileMapper {

    private final DspBaseWebhookAddress webhookAddress;
    private final DefaultParticipantIdExtractionFunction idExtractionFunction;

    DataspaceProfileMapper(DspBaseWebhookAddress webhookAddress, DefaultParticipantIdExtractionFunction idExtractionFunction) {
        this.webhookAddress = webhookAddress;
        this.idExtractionFunction = idExtractionFunction;
    }

    DataspaceProfileContext toContext(DataspaceProfile profile) {
        var namespace = new JsonLdNamespace(profile.getNamespace());
        var path = profile.getPath() == null || profile.getPath().isBlank()
                ? "/" + profile.getName()
                : profile.getPath();
        var protocolVersion = new ProtocolVersion(profile.getProtocolVersion(), path, profile.getBinding());

        return new DataspaceProfileContext(
                profile.getName(),
                protocolVersion,
                () -> webhookAddress.get() + "/" + profile.getName(),
                idExtractionFunction,
                namespace,
                profile.getJsonLdContextsUrl());
    }
}
