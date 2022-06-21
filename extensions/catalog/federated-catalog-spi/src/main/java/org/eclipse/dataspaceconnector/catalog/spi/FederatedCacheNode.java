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

package org.eclipse.dataspaceconnector.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Object that contains information of a FederatedCacheNode. This is used by the {@link FederatedCacheNodeDirectory}.
 */
public class FederatedCacheNode {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("url")
    private final String targetUrl;
    @JsonProperty("supportedProtocols")
    private final List<String> supportedProtocols;


    @JsonCreator
    public FederatedCacheNode(@JsonProperty("name") String name,
                              @JsonProperty("url") String targetUrl,
                              @JsonProperty("supportedProtocols") List<String> supportedProtocols) {
        this.name = name;
        this.targetUrl = targetUrl;
        this.supportedProtocols = supportedProtocols;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public String getName() {
        return name;
    }
}
