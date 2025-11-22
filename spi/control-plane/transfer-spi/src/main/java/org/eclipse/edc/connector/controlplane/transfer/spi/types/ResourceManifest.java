/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A collection of resources to be provisioned to support a data transfer, e.g. a data destination such as an object storage container.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:resourcemanifest")
@JsonDeserialize(builder = ResourceManifest.Builder.class)
public class ResourceManifest {
    private String transferProcessId;
    private final List<ResourceDefinition> definitions = new ArrayList<>();

    private ResourceManifest() {

    }

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
        private final ResourceManifest manifest;

        private Builder() {
            manifest = new ResourceManifest();
        }

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
    }
}
