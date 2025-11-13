/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.asset.spi.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = DataplaneMetadata.Builder.class)
public class DataplaneMetadata {

    public static final String EDC_DATAPLANE_METADATA_LABELS = EDC_NAMESPACE + "labels";
    public static final String EDC_DATAPLANE_METADATA_PROPERTIES = EDC_NAMESPACE + "properties";

    private final List<String> labels = new ArrayList<>();
    private final Map<String, Object> properties = new HashMap<>();

    private DataplaneMetadata() {

    }

    public List<String> getLabels() {
        return labels;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return labels.isEmpty() && properties.isEmpty();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DataplaneMetadata instance = new DataplaneMetadata();

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public DataplaneMetadata build() {
            return instance;
        }

        public Builder properties(Map<String, Object> properties) {
            instance.properties.putAll(properties);
            return this;
        }

        public Builder property(String key, Object value) {
            instance.properties.put(key, value);
            return this;
        }

        public Builder labels(List<String> labels) {
            instance.labels.addAll(labels);
            return this;
        }

        public Builder label(String label) {
            instance.labels.add(label);
            return this;
        }
    }
}
