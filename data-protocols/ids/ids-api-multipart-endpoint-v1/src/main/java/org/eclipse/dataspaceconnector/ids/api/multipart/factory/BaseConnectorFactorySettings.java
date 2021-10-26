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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.factory;

import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class BaseConnectorFactorySettings {
    private final URI id;
    private final String title;
    private final String description;
    private final URI maintainer;
    private final URI curator;
    private final URI connectorEndpoint;
    private final SecurityProfile securityProfile;

    private BaseConnectorFactorySettings(@Nullable URI id,
                                         @Nullable String title,
                                         @Nullable String description,
                                         @Nullable URI maintainer,
                                         @Nullable URI curator,
                                         @Nullable URI connectorEndpoint,
                                         @Nullable SecurityProfile securityProfile) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.maintainer = maintainer;
        this.curator = curator;
        this.connectorEndpoint = connectorEndpoint;
        this.securityProfile = securityProfile;
    }

    @Nullable
    public URI getId() {
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
    public URI getMaintainer() {
        return maintainer;
    }

    @Nullable
    public URI getCurator() {
        return curator;
    }

    @Nullable
    public URI getConnectorEndpoint() {
        return connectorEndpoint;
    }

    @Nullable
    public SecurityProfile getSecurityProfile() {
        return securityProfile;
    }

    public static class Builder {
        private URI id;
        private String title;
        private String description;
        private URI maintainer;
        private URI curator;
        private URI connectorEndpoint;
        private SecurityProfile securityProfile;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder id(@Nullable URI id) {
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

        public Builder maintainer(@Nullable URI maintainer) {
            this.maintainer = maintainer;
            return this;
        }

        public Builder curator(@Nullable URI curator) {
            this.curator = curator;
            return this;
        }

        public Builder connectorEndpoint(@Nullable URI connectorEndpoint) {
            this.connectorEndpoint = connectorEndpoint;
            return this;
        }

        public Builder securityProfile(@Nullable SecurityProfile securityProfile) {
            this.securityProfile = securityProfile;
            return this;
        }

        public BaseConnectorFactorySettings build() {
            return new BaseConnectorFactorySettings(id, title, description, maintainer, curator, connectorEndpoint, securityProfile);
        }
    }
}
