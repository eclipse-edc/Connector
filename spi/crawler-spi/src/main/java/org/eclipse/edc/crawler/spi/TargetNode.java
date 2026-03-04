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
 * Represents an abstract crawl target, i.e. some endpoint, that a crawler can target using a specific {@link CrawlerAction}.
 * All {@link TargetNode} entries are maintained in a {@link TargetNodeDirectory}.
 */
public record TargetNode(@JsonProperty("name") String name,
                         @JsonProperty("id") String id,
                         @JsonProperty("url") String targetUrl,
                         @JsonProperty("supportedProtocols") List<String> supportedProtocols) {
    @JsonCreator
    public TargetNode {
    }
}
