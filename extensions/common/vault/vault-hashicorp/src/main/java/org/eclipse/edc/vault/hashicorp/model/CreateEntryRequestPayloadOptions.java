/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class CreateEntryRequestPayloadOptions {

    private Integer cas;

    CreateEntryRequestPayloadOptions() {
    }

    public Integer getCas() {
        return this.cas;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final CreateEntryRequestPayloadOptions createEntryRequestPayloadOptions;

        private Builder() {
            createEntryRequestPayloadOptions = new CreateEntryRequestPayloadOptions();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder cas(Integer cas) {
            createEntryRequestPayloadOptions.cas = cas;
            return this;
        }

        public CreateEntryRequestPayloadOptions build() {
            return createEntryRequestPayloadOptions;
        }
    }
}
