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

package org.eclipse.edc.statemachine.retry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.StatefulEntity;

class TestEntity extends StatefulEntity<TestEntity> {
    @Override
    public TestEntity copy() {
        return this;
    }

    @Override
    public String stateAsString() {
        return "STATE";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<TestEntity, Builder> {

        private Builder(TestEntity entity) {
            super(entity);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new TestEntity());
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        protected TestEntity build() {
            return super.build();
        }
    }
}
