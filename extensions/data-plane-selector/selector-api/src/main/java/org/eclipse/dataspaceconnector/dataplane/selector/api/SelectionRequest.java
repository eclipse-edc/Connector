/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.selector.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

/**
 * Represents the request body that the {@link DataplaneSelectorApiController#find(SelectionRequest)} endpoint requires
 * Contains source and destination address and optionally the name of a selection strategy
 */
public class SelectionRequest {
    private final DataAddress source;
    private final DataAddress destination;
    private String strategy;


    public SelectionRequest(DataAddress source,
                            DataAddress destination) {
        this.source = source;
        this.destination = destination;
    }

    @JsonCreator
    public SelectionRequest(@JsonProperty("source") DataAddress source,
                            @JsonProperty("destination") DataAddress destination,
                            @JsonProperty("strategy") String strategy) {
        this(source, destination);
        this.strategy = strategy;
    }


    public DataAddress getSource() {
        return source;
    }

    public DataAddress getDestination() {
        return destination;
    }

    public String getStrategy() {
        return strategy;
    }

}
