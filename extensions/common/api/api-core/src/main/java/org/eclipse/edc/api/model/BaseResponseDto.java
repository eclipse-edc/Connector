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

public class BaseResponseDto {
    protected long createdAt;

    public long getCreatedAt() {
        return createdAt;
    }

    protected abstract static class Builder<D extends BaseResponseDto, B extends Builder<D, B>> {
        protected final D dto;

        protected Builder(D dto) {
            this.dto = dto;
        }

        public abstract B self();


        public B createdAt(long createdAt) {
            dto.createdAt = createdAt;
            return self();
        }

        public D build() {
            return dto;
        }
    }
}
