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

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a Dataspace Profile Context: the binding of a dataspace identity (JSON-LD namespace +
 * context document) to a DSP protocol version and an identity-extraction strategy.
 *
 * <p>The {@link #name} is used as the profile segment in DSP HTTP paths in virtual mode
 * ({@code /{participantContextId}/{profileId}/...}) and as the suffix of the protocol string
 * ({@code dataspace-protocol-http:{profileId}}).
 *
 * @param name                 the profile identifier; it may appear in URL paths for multiple profile support.
 * @param protocolVersion      the DSP protocol version this profile binds to.
 * @param webhook              the protocol endpoint URL.
 * @param idExtractionFunction extracts a participant id from a verified ClaimToken.
 * @param protocolNamespace    the JSON-LD namespace of the dataspace.
 * @param jsonLdContextsUrl    URL of the JSON-LD context document used for compaction.
 */
public record DataspaceProfileContext(String name,
                                      ProtocolVersion protocolVersion,
                                      ProtocolWebhook webhook,
                                      ParticipantIdExtractionFunction idExtractionFunction,
                                      JsonLdNamespace protocolNamespace,
                                      List<String> jsonLdContextsUrl) {

    public static final String DATASPACE_PROFILE_CONTEXT_TYPE_TERM = "DataspaceProfile";
    public static final String DATASPACE_PROFILE_CONTEXT_TYPE_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_TYPE_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_NAME_TERM = "name";
    public static final String DATASPACE_PROFILE_CONTEXT_NAME_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_NAME_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_TERM = "protocol";
    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_PROTOCOL_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_TERM = "version";
    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_TERM = "path";
    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_TERM = "binding";
    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_TERM = "namespace";
    public static final String DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_TERM;

    public static final String DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_TERM = "jsonLdContextsUrl";
    public static final String DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI = EDC_NAMESPACE + DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_TERM;

}
