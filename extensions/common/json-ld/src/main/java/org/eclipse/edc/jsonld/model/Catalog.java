package org.eclipse.edc.jsonld.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 *
 */
@JsonDeserialize(builder = Dataset.Builder.class)
public class Catalog extends Entity {
    private final List<Dataset> datasets = new ArrayList<>();
    private final Map<String, Object> properties = new HashMap<>();

    private Catalog() {
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonIgnore
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Entity.Builder<Catalog, Builder> {

        protected Builder(Catalog catalog) {
            super(catalog);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Catalog());
        }

        @Override
        public Builder id(String id) {
            entity.id = id;
            return self();
        }

        @Override
        public Builder createdAt(long value) {
            entity.createdAt = value;
            return self();
        }

        @Override
        public Catalog build() {
            if (entity.getId() == null) {
                id(UUID.randomUUID().toString());
            }
            return super.build();
        }

        public Builder properties(Map<String, Object> properties) {
            Objects.requireNonNull(properties);
            entity.properties.putAll(properties);
            return self();
        }

        public Builder datasets(List<Dataset> contractOffers) {
            Objects.requireNonNull(contractOffers);
            entity.datasets.addAll(contractOffers);
            return self();
        }

        public Builder dataset(Dataset contractOffer) {
            Objects.requireNonNull(contractOffer);
            entity.datasets.add(contractOffer);
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return self();
        }

    }
}
