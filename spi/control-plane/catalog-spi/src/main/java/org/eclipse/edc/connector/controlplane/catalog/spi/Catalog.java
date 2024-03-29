/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add datasets
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.UUID.randomUUID;

/**
 * Entity representing a Catalog
 */
@JsonDeserialize(builder = Catalog.Builder.class)
public class Catalog {
    private String id;
    private List<Dataset> datasets;
    private List<DataService> dataServices;
    private Map<String, Object> properties;
    private String participantId;

    public String getId() {
        return id;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public List<DataService> getDataServices() {
        return dataServices;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getParticipantId() {
        return participantId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Catalog catalog;

        private Builder() {
            catalog = new Catalog();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            catalog.id = id;
            return this;
        }

        public Builder datasets(List<Dataset> datasets) {
            catalog.datasets = datasets;
            return this;
        }

        public Builder dataset(Dataset dataset) {
            if (catalog.datasets == null) {
                catalog.datasets = new ArrayList<>();
            }
            catalog.datasets.add(dataset);
            return this;
        }

        public Builder dataServices(List<DataService> dataServices) {
            catalog.dataServices = dataServices;
            return this;
        }

        public Builder dataService(DataService dataService) {
            if (catalog.dataServices == null) {
                catalog.dataServices = new ArrayList<>();
            }
            catalog.dataServices.add(dataService);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            catalog.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            if (catalog.properties == null) {
                catalog.properties = new HashMap<>();
            }
            catalog.properties.put(key, value);
            return this;
        }

        public Builder participantId(String participantId) {
            catalog.participantId = participantId;
            return this;
        }

        public Catalog build() {
            if (catalog.id == null) {
                catalog.id = randomUUID().toString();
            }

            return catalog;
        }
    }
}
