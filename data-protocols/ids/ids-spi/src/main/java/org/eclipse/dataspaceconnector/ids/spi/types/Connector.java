/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.types;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

/**
 * DTO representing IDS Dataspace Connector
 */
@JsonDeserialize(builder = Connector.Builder.class)
public class Connector {
    private final String id;
    private final String title;
    private final String description;
    private final String connectorVersion;
    private final SecurityProfile securityProfile;
    private final List<Catalog> dataCatalogs;
    private final URI endpoint;
    private final URI maintainer;
    private final URI curator;

    public Connector(
            @Nullable String id,
            @Nullable String title,
            @Nullable String description,
            @Nullable String connectorVersion,
            @Nullable SecurityProfile securityProfile,
            @Nullable List<Catalog> dataCatalogs,
            @Nullable URI endpoint,
            @Nullable URI maintainer,
            @Nullable URI curator) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.connectorVersion = connectorVersion;
        this.securityProfile = securityProfile;
        this.dataCatalogs = dataCatalogs;
        this.endpoint = endpoint;
        this.maintainer = maintainer;
        this.curator = curator;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getConnectorVersion() {
        return connectorVersion;
    }

    @Nullable
    public SecurityProfile getSecurityProfile() {
        return securityProfile;
    }

    @Nullable
    public List<Catalog> getDataCatalogs() {
        return dataCatalogs;
    }

    @Nullable
    public URI getEndpoint() {
        return endpoint;
    }

    @Nullable
    public URI getMaintainer() {
        return maintainer;
    }

    @Nullable
    public URI getCurator() {
        return curator;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String id;
        private String title;
        private String description;
        private String connectorVersion;
        private SecurityProfile securityProfile;
        private List<Catalog> dataCatalogs;
        private URI endpoint;
        private URI maintainer;
        private URI curator;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public Builder title(@Nullable String title) {
            this.title = title;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder connectorVersion(@Nullable String connectorVersion) {
            this.connectorVersion = connectorVersion;
            return this;
        }

        public Builder securityProfile(@Nullable SecurityProfile securityProfile) {
            this.securityProfile = securityProfile;
            return this;
        }

        public Builder dataCatalogs(@Nullable List<Catalog> dataCatalogs) {
            this.dataCatalogs = dataCatalogs;
            return this;
        }

        public Builder endpoint(@Nullable URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder maintainer(@Nullable URI maintainer) {
            this.maintainer = maintainer;
            return this;
        }

        public Builder curator(@Nullable URI curator) {
            this.curator = curator;
            return this;
        }

        @NotNull
        public Connector build() {
            return new Connector(id, title, description, connectorVersion, securityProfile, dataCatalogs, endpoint, maintainer, curator);
        }
    }
}
