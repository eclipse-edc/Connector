package org.eclipse.edc.jsonld.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dataset {
    private String id;

    public String getId() {
        return id;
    }

    private List<Policy> offers = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();

    public List<Policy> getOffers() {
        return offers;
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

        public Builder offer(Policy policy) {
            dataset.offers.add(policy);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            dataset.properties = properties;
            return this;
        }

        public Dataset build() {
            return dataset;
        }
    }

}
