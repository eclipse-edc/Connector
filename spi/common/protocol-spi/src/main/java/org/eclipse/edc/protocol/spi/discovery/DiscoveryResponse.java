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

package org.eclipse.edc.protocol.spi.discovery;

import org.eclipse.edc.protocol.spi.DataspaceProfileContext;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Result of a discovery request: a local dataspace profile that matches a protocol version
 * advertised by the counterparty in its {@code /.well-known/dspace-version} document.
 *
 * @param profile          the local profile id (i.e. the value of
 *                         {@link DataspaceProfileContext#name()}).
 * @param counterPartyPath the path under the counterparty's base URL where the matching DSP
 *                         version is exposed (as returned in the well-known versions document).
 * @param binding          the protocol binding for this match (copied from the counterparty
 *                         version entry).
 */
public record DiscoveryResponse(String profile,
                                String version,
                                String counterPartyPath,
                                String binding) {

    public static final String DISCOVERY_RESPONSE_TYPE_TERM = "DiscoveryResponse";
    public static final String DISCOVERY_RESPONSE_TYPE_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_TYPE_TERM;

    public static final String DISCOVERY_RESPONSE_PROFILE_TERM = "profile";
    public static final String DISCOVERY_RESPONSE_PROFILE_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_PROFILE_TERM;

    public static final String DISCOVERY_RESPONSE_VERSION_TERM = "version";
    public static final String DISCOVERY_RESPONSE_VERSION_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_VERSION_TERM;

    public static final String DISCOVERY_RESPONSE_COUNTER_PARTY_PATH_TERM = "counterPartyPath";
    public static final String DISCOVERY_RESPONSE_COUNTER_PARTY_PATH_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_COUNTER_PARTY_PATH_TERM;

    public static final String DISCOVERY_RESPONSE_BINDING_TERM = "binding";
    public static final String DISCOVERY_RESPONSE_BINDING_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_BINDING_TERM;
}
