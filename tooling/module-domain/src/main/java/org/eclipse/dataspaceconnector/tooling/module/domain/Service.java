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
 * An extension point defined in an SPI module or a service provided an extension module.
 */
public class Service {
    private String service;

    public Service(@JsonProperty("service") String service) {
        this.service = requireNonNull(service, "service");
    }

    /**
     * Returns the service class name.
     */
    public String getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var service1 = (Service) o;
        return service.equals(service1.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service);
    }
}
