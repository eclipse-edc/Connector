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

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representation of a Dataset.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DatasetDto extends Typed {

    @JsonProperty("hasPolicy")
    private List<OfferDto> offers;
    @JsonProperty("@id")
    private String id;


    public DatasetDto() {
        super("Dataset");
    }

    public DatasetDto(String id,
                      List<OfferDto> offers) {
        super("Dataset");
        this.id = id;
        this.offers = offers;
    }

    public String id() {
        return id;
    }

    public List<OfferDto> offers() {
        return offers;
    }

}
