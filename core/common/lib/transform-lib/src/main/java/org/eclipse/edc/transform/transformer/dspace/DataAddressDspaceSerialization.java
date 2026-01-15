/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform.transformer.dspace;

/**
 * Contains constants specifically intended for serializing a {@link org.eclipse.edc.spi.types.domain.DataAddress}
 * to JSON-LD using the `dspace:` prefix format.
 */
public interface DataAddressDspaceSerialization {
    String DSPACE_DATAADDRESS_TYPE_TERM = "DataAddress";
    String ENDPOINT_TYPE_PROPERTY_TERM = "endpointType";
    String ENDPOINT_PROPERTY_TERM = "endpoint";
    String ENDPOINT_PROPERTIES_PROPERTY_TERM = "endpointProperties";
    String ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM = "EndpointProperty";
    String ENDPOINT_PROPERTY_NAME_PROPERTY_TERM = "name";
    String ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM = "value";
}
