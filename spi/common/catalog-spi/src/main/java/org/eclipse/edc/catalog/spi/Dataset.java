/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.UUID.randomUUID;

/**
 * Models the Dataset class of the DCAT spec. A Dataset is defined as a collection of data
 * available for access or download. The data described by a Dataset may be offered under
 * different policies and may be available via different distributions.
 */
@JsonDeserialize(builder = Dataset.Builder.class)
public class Dataset {

    private String id;
    
    /** Policies under which this Dataset is available. */
    private Map<String, Policy> offers;
    
    /** Representations of this Dataset. */
    private List<Distribution> distributions;
    
    /** Properties for describing the Dataset. */
    private Map<String, Object> properties = new HashMap<>();

    public String getId() {
        return id;
    }

    public Map<String, Policy> getOffers() {
        return offers;
    }

    public List<Distribution> getDistributions() {
        return distributions;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonIgnore
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Dataset dataset;

        private Builder() {
            dataset = new Dataset();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dataset.id = id;
            return this;
        }

        public Builder offer(String offerId, Policy policy) {
            if (dataset.offers == null) {
                dataset.offers = new HashMap<>();
            }
            dataset.offers.put(offerId, policy);
            return this;
        }

        public Builder distribution(Distribution distribution) {
            if (dataset.distributions == null) {
                dataset.distributions = new ArrayList<>();
            }
            dataset.distributions.add(distribution);
            return this;
        }

        public Builder distributions(List<Distribution> distributions) {
            if (dataset.distributions == null) {
                dataset.distributions = new ArrayList<>();
            }
            dataset.distributions.addAll(distributions);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            dataset.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            dataset.properties.put(key, value);
            return this;
        }

        public Dataset build() {
            if (dataset.id == null) {
                dataset.id = randomUUID().toString();
            }

            Objects.requireNonNull(dataset.offers, "At least one offer required for Dataset.");
            Objects.requireNonNull(dataset.distributions, "At least one Distribution required for Dataset.");

            return dataset;
        }
    }

}
