/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

import static java.util.UUID.randomUUID;

/**
 * Models the DataService class of the DCAT spec. A DataService is defined as a collection
 * of operations that provide access to Datasets. A DataService specifies the endpoint for
 * initiating a contract negotiation and transfer.
 */
@JsonDeserialize(builder = DataService.Builder.class)
public class DataService {

    private String id;

    /**
     * Type of access service, e.g. a connector.
     */
    private String terms;

    /**
     * Endpoint for accessing associated Distributions.
     */
    private String endpointUrl;

    public String getId() {
        return id;
    }

    public String getEndpointDescription() {
        return terms;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.hash(id, terms, endpointUrl));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var dataService = (DataService) o;
        return id.equals(dataService.getId()) && terms.equals(dataService.getEndpointDescription()) && endpointUrl.equals(dataService.getEndpointUrl());
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataService dataService;

        private Builder() {
            dataService = new DataService();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dataService.id = id;
            return this;
        }

        public Builder endpointDescription(String terms) {
            dataService.terms = terms;
            return this;
        }

        public Builder endpointUrl(String endpointUrl) {
            dataService.endpointUrl = endpointUrl;
            return this;
        }

        public DataService build() {
            if (dataService.id == null) {
                dataService.id = randomUUID().toString();
            }

            return dataService;
        }
    }

}
