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

package org.eclipse.edc.connector.contract.spi.types.offer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;

public class Dataset {
    
    private String id;
    private Map<String, Policy> offers = new HashMap<>();
    private List<Distribution> distributions = new ArrayList<>();
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
            dataset.offers.put(offerId, policy);
            return this;
        }
        
        public Builder distributions(List<Distribution> distributions) {
            dataset.distributions = distributions;
            return this;
        }
        
        public Builder distribution(Distribution distribution) {
            dataset.distributions.add(distribution);
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
            return dataset;
        }
    }
    
}
