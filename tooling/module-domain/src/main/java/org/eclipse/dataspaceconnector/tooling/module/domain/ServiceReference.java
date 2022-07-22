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

package org.eclipse.dataspaceconnector.tooling.module.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A service required by an {@link EdcModule}.
 */
public class ServiceReference {
    private String service;
    private boolean required;

    public ServiceReference(@JsonProperty("service") String service, @JsonProperty("required") boolean required) {
        this.service = requireNonNull(service, "service");
        this.required = required;
    }

    /**
     * Returns the service class name.
     */
    public String getService() {
        return service;
    }

    /**
     * Returns true if the service must be provided.
     */
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ServiceReference) o;
        return required == that.required && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, required);
    }
}
