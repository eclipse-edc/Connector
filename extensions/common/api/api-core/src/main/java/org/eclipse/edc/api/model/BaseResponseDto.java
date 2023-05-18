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

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

public class BaseResponseDto extends BaseDto {

    @JsonProperty(value = ID)
    protected String id;

    protected long createdAt;

    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    protected abstract static class Builder<D extends BaseResponseDto, B extends Builder<D, B>> {
        protected final D dto;

        protected Builder(D dto) {
            this.dto = dto;
        }

        public abstract B self();

        @JsonProperty(value = ID)
        public B id(String id) {
            dto.id = id;
            return self();
        }

        public B createdAt(long createdAt) {
            dto.createdAt = createdAt;
            return self();
        }

        public D build() {
            return dto;
        }
    }
}
