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

package org.eclipse.edc.crawler.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.crawler.spi.CrawlerAction;

/**
 * {@link CrawlerAction}s return {@code UpdateResponse} objects after it completes. Contains information about the {@code source}, i.e. where the response comes from
 * <p>
 * Implementors of the {@link CrawlerAction} are expected to provide their specialization of the UpdateResponse
 */
public abstract class UpdateResponse {
    private final String source;

    @JsonCreator
    public UpdateResponse(@JsonProperty("source") String source) {
        this.source = source;
    }

    /**
     * The URL from which the catalog update originates, i.e. the catalog's sender
     */
    public String getSource() {
        return source;
    }
}
