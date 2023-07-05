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

package org.eclipse.edc.connector.api.management.asset.model;

import org.eclipse.edc.api.model.BaseDto;

import java.util.HashMap;
import java.util.Map;

@Deprecated(since = "0.1.3")
public abstract class AssetRequestDto extends BaseDto {

    protected Map<String, Object> properties;

    protected Map<String, Object> privateProperties = new HashMap<>();

    protected abstract static class Builder<A extends AssetRequestDto, B extends Builder<A, B>> {

        protected final A dto;

        protected Builder(A dto) {
            this.dto = dto;
        }


        public B properties(Map<String, Object> properties) {
            dto.properties = properties;
            return self();
        }

        public B privateProperties(Map<String, Object> privateProperties) {
            dto.privateProperties = privateProperties;
            return self();
        }

        public abstract B self();

        public A build() {
            return dto;
        }
    }
}
