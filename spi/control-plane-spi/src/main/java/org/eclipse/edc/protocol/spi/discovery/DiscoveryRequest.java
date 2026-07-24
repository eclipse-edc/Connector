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

import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a request to discover the dataspace profiles that can be used to communicate with a
 * counterparty. Exactly one of {@link #counterPartyId} (a DID) or {@link #counterPartyAddress}
 * (a URL that exposes {@code /.well-known/dspace-version}) must be provided.
 *
 * @param counterPartyId      the counterparty's DID; when set, the DID is resolved and the
 *                            {@code DataService} entry of the DID document provides the base URL
 *                            of the well-known endpoint.
 * @param counterPartyAddress the counterparty's discovery URL pointing directly at the
 *                            {@code /.well-known/dspace-version} endpoint (or the host that
 *                            serves it). Takes precedence over {@link #counterPartyId}.
 */
public record DiscoveryRequest(@Nullable String counterPartyId,
                               @Nullable String counterPartyAddress) {

    public static final String DISCOVERY_REQUEST_TYPE_TERM = "DiscoveryRequest";
    public static final String DISCOVERY_REQUEST_TYPE_IRI = EDC_NAMESPACE + DISCOVERY_REQUEST_TYPE_TERM;

    public static final String DISCOVERY_REQUEST_COUNTER_PARTY_ID_TERM = "counterPartyId";
    public static final String DISCOVERY_REQUEST_COUNTER_PARTY_ID_IRI = EDC_NAMESPACE + DISCOVERY_REQUEST_COUNTER_PARTY_ID_TERM;

    public static final String DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_TERM = "counterPartyAddress";
    public static final String DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_IRI = EDC_NAMESPACE + DISCOVERY_REQUEST_COUNTER_PARTY_ADDRESS_TERM;
}
