/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A collection of resources to be provisioned to support a data transfer, e.g. a data destination such as an object storage container.
 */
@JsonTypeName("dagx:resourcemanifest")
@JsonDeserialize(builder = ResourceManifest.Builder.class)
public class ResourceManifest {
    private String transferProcessId;
    private List<ResourceDefinition> definitions = new ArrayList<>();

    @NotNull
    public List<ResourceDefinition> getDefinitions() {
        return definitions;
    }

    public void addDefinition(ResourceDefinition definition) {
        if (transferProcessId != null) {
            definition.setTransferProcessId(transferProcessId);
        }
        definitions.add(definition);
    }

    public boolean empty() {
        return definitions.isEmpty();
    }

    void setTransferProcessId(String transferProcessId) {
        Objects.requireNonNull(transferProcessId, "transferProcessId");
        this.transferProcessId = transferProcessId;
        definitions.forEach(d -> d.setTransferProcessId(transferProcessId));
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ResourceManifest manifest;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder definitions(List<ResourceDefinition> definitions) {
            manifest.definitions.addAll(definitions);
            return this;
        }

        public ResourceManifest build() {
            return manifest;
        }

        private Builder() {
            manifest = new ResourceManifest();
        }
    }
}
