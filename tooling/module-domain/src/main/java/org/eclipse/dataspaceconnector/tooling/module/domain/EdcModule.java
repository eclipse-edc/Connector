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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.tooling.module.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * An EDC module.
 */
@JsonDeserialize(builder = EdcModule.Builder.class)
public class EdcModule {
    private String id;
    private String version;
    private String name;
    private ModuleType type = ModuleType.EXTENSION;
    private List<String> categories = new ArrayList<>();
    private List<Service> extensionPoints = new ArrayList<>();
    private List<Service> provides = new ArrayList<>();
    private List<ServiceReference> references = new ArrayList<>();
    private List<ConfigurationSetting> configuration = new ArrayList<>();
    private String overview;

    /**
     * Returns the module id, which corresponds to Maven-style <code>group:artifact</code> coordinates.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the module version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the module readable name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the module type.
     */
    public ModuleType getType() {
        return type;
    }

    /**
     * Returns categories assigned to the module, or an empty collection.
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Returns services provided by this extension module, or an empty collection.
     */
    public List<Service> getProvides() {
        return provides;
    }

    /**
     * Returns services extension points defined in this SPI module, or an empty collection.
     */
    public List<Service> getExtensionPoints() {
        return extensionPoints;
    }

    /**
     * Returns services that are provided by other modules and referenced in the current module, or an empty collection.
     */
    public List<ServiceReference> getReferences() {
        return references;
    }

    /**
     * Returns the configuration settings for this module.
     */
    public List<ConfigurationSetting> getConfiguration() {
        return configuration;
    }

    /**
     * Returns a Markdown-formatted description of this module.
     */
    public String getOverview() {
        return overview;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var edcModule = (EdcModule) o;
        return id.equals(edcModule.id) && version.equals(edcModule.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private EdcModule module;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            module.id = id;
            return this;
        }

        public Builder version(String version) {
            module.version = version;
            return this;
        }

        public Builder name(String name) {
            module.name = name;
            return this;
        }

        public Builder type(ModuleType type) {
            module.type = type;
            return this;
        }

        public Builder categories(List<String> categories) {
            module.categories.addAll(categories);
            return this;
        }

        public Builder provides(List<Service> provides) {
            module.provides.addAll(provides);
            return this;
        }

        public Builder extensionPoints(List<Service> provides) {
            module.extensionPoints.addAll(provides);
            return this;
        }

        public Builder references(List<ServiceReference> requires) {
            module.references.addAll(requires);
            return this;
        }

        public Builder configuration(List<ConfigurationSetting> configuration) {
            module.configuration.addAll(configuration);
            return this;
        }

        public Builder overview(String overview) {
            module.overview = overview;
            return this;
        }

        public EdcModule build() {
            requireNonNull(module.id, "id");
            requireNonNull(module.version, "version");
            requireNonNull(module.name, "name");
            return module;
        }

        private Builder() {
            module = new EdcModule();
        }

    }
}
