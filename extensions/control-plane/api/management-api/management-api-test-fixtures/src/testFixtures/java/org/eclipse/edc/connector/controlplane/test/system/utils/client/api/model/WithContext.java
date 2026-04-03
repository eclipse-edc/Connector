/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;

/**
 * Wrapper to add JSON-LD @context to any Typed entity.
 */
public record WithContext<T extends Typed>(@JsonUnwrapped T entity, @JsonProperty("@context") List<String> context) {
    public WithContext(T entity) {
        this(entity, List.of(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2));
    }
}
