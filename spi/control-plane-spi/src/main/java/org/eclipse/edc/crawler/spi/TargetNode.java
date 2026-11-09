/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.crawler.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a crawl target, i.e. some endpoint that the catalog crawler can query using one of its supported protocols.
 * All {@link TargetNode} entries are maintained in a {@link TargetNodeDirectory}, scoped to the local participant context
 * (identified by {@code participantContextId}) on whose behalf the node is crawled.
 * <p>
 * While the {@code id} uniquely identifies the node within the directory, the {@code counterPartyId} represents the
 * identity of the counter party (used, for example, as the {@code aud} claim of the token presented to the node). This
 * allows targeting multiple catalogs served by the same identity: each catalog gets its own {@code id} while sharing the
 * same {@code counterPartyId}. When not set, {@code counterPartyId} falls back to the {@code id}.
 */
public record TargetNode(@JsonProperty("name") String name,
                         @JsonProperty("id") String id,
                         @JsonProperty("counterPartyId") String counterPartyId,
                         @JsonProperty("url") String targetUrl,
                         @JsonProperty("participantContextId") String participantContextId,
                         @JsonProperty("supportedProtocols") List<String> supportedProtocols) {
    @JsonCreator
    public TargetNode {
        counterPartyId = counterPartyId == null ? id : counterPartyId;
    }

    /**
     * Convenience constructor that defaults the {@code counterPartyId} to the given {@code id}.
     *
     * @deprecated counterPartyId should be set accordingly
     */
    @Deprecated(since = "1.0.0")
    public TargetNode(String name, String id, String targetUrl, String participantContextId, List<String> supportedProtocols) {
        this(name, id, null, targetUrl, participantContextId, supportedProtocols);
    }
}
