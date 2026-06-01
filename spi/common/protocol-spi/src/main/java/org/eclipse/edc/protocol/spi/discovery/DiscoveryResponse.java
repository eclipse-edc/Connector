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
 * @param profile      the local profile id (i.e. the value of
 *                     {@link DataspaceProfileContext#name()}).
 * @param version      the DSP protocol version shared by the local profile and the
 *                     counterparty entry.
 * @param counterParty the counterparty coordinates for this match, see {@link CounterParty}.
 * @param binding      the protocol binding for this match (copied from the counterparty
 *                     version entry).
 */
public record DiscoveryResponse(String profile,
                                String version,
                                CounterParty counterParty,
                                String binding) {

    public static final String DISCOVERY_RESPONSE_TYPE_TERM = "DiscoveryResponse";
    public static final String DISCOVERY_RESPONSE_TYPE_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_TYPE_TERM;

    public static final String DISCOVERY_RESPONSE_PROFILE_TERM = "profile";
    public static final String DISCOVERY_RESPONSE_PROFILE_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_PROFILE_TERM;

    public static final String DISCOVERY_RESPONSE_VERSION_TERM = "version";
    public static final String DISCOVERY_RESPONSE_VERSION_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_VERSION_TERM;

    public static final String DISCOVERY_RESPONSE_COUNTER_PARTY_TERM = "counterParty";
    public static final String DISCOVERY_RESPONSE_COUNTER_PARTY_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_COUNTER_PARTY_TERM;

    public static final String DISCOVERY_RESPONSE_BINDING_TERM = "binding";
    public static final String DISCOVERY_RESPONSE_BINDING_IRI = EDC_NAMESPACE + DISCOVERY_RESPONSE_BINDING_TERM;

    public static final String COUNTER_PARTY_PATH_TERM = "path";
    public static final String COUNTER_PARTY_PATH_IRI = EDC_NAMESPACE + COUNTER_PARTY_PATH_TERM;

    public static final String COUNTER_PARTY_DATASERVICE_ENDPOINT_TERM = "dataServiceEndpoint";
    public static final String COUNTER_PARTY_DATASERVICE_ENDPOINT_IRI = EDC_NAMESPACE + COUNTER_PARTY_DATASERVICE_ENDPOINT_TERM;

    /**
     * Counterparty coordinates for a matched protocol version.
     *
     * @param path                the path under the counterparty's base URL where the matching DSP
     *                            version is exposed (as returned in the well-known versions document).
     * @param dataServiceEndpoint the counterparty's data service endpoint URL.
     */
    public record CounterParty(String path, String dataServiceEndpoint) {
    }
}
