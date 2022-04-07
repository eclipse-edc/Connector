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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Failure;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the result of one single health check, like a heartbeat to a database or to a messaging service.
 */
public class HealthCheckResult extends AbstractResult<Boolean, Failure> {
    private String component;

    private HealthCheckResult(boolean successful, Failure failure) {
        super(successful, failure);
    }

    public static HealthCheckResult success() {
        return new HealthCheckResult(true, null);
    }

    public static HealthCheckResult failed(String... errors) {
        var errorList = Stream.of(errors).filter(Objects::nonNull).collect(Collectors.toList());
        return new HealthCheckResult(false, new Failure(errorList));
    }

    @JsonProperty("isHealthy")
    @Override
    public @NotNull Boolean getContent() {
        return super.getContent();
    }

    public String getComponent() {
        return component;
    }

    public HealthCheckResult forComponent(String component) {
        this.component = component;
        return this;
    }

    public static class Builder {
        private String component;
        private boolean success = true;
        private Failure failure;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder success() {
            success = true;
            return this;
        }

        public Builder success(boolean success, String... errors) {
            this.success = success;
            if (!success) {
                return failure(errors);
            }
            return this;
        }

        public Builder failure(String... errors) {
            var errorList = Stream.of(errors).filter(Objects::nonNull).collect(Collectors.toList());
            failure = new Failure(errorList);
            success = false;
            return this;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public HealthCheckResult build() {
            var hc = new HealthCheckResult(success, failure);
            hc.component = component;

            return hc;
        }
    }


}
