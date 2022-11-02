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

package org.eclipse.edc.protocol.ids.spi.types.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@JsonDeserialize(builder = ArtifactRequestMessagePayload.Builder.class)
public class ArtifactRequestMessagePayload {
    private final DataAddress dataDestination;
    private final String secret;

    private ArtifactRequestMessagePayload(@NotNull DataAddress dataDestination, @Nullable String secret) {
        this.dataDestination = Objects.requireNonNull(dataDestination);
        this.secret = secret;
    }

    @NotNull
    public DataAddress getDataDestination() {
        return dataDestination;
    }

    @Nullable
    public String getSecret() {
        return secret;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private DataAddress dataDestination;
        private String secret;

        private Builder() {
        }

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataDestination(DataAddress dataDestination) {
            this.dataDestination = dataDestination;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public ArtifactRequestMessagePayload build() {
            return new ArtifactRequestMessagePayload(dataDestination, secret);
        }

    }
}
