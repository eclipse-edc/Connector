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

package org.eclipse.dataspaceconnector.api.model;

public class MutableDto extends BaseResponseDto {
    protected long updatedAt;

    public long getUpdatedAt() {
        return updatedAt;
    }

    protected abstract static class Builder<D extends MutableDto, B extends Builder<D, B>> extends BaseResponseDto.Builder<D, B> {

        protected Builder(D dto) {
            super(dto);
        }

        public B updatedAt(long time) {
            dto.updatedAt = time;
            return self();
        }

    }
}
