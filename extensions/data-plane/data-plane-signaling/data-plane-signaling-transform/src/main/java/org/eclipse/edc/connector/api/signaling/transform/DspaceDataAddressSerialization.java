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

package org.eclipse.edc.connector.api.signaling.transform;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Contains constants specifically intended for serializing a {@link org.eclipse.edc.spi.types.domain.DataAddress}
 * to JSON-LD using the `dspace:` prefix format.
 */
public interface DspaceDataAddressSerialization {
    String DSPACE_DATAADDRESS_TYPE = DSPACE_SCHEMA + "DataAddress";
    String ENDPOINT_TYPE_PROPERTY = DSPACE_SCHEMA + "endpointType";
    String ENDPOINT_PROPERTY = DSPACE_SCHEMA + "endpoint";
    String ENDPOINT_PROPERTIES_PROPERTY = DSPACE_SCHEMA + "endpointProperties";
    String ENDPOINT_PROPERTY_PROPERTY_TYPE = DSPACE_SCHEMA + "EndpointProperty";
    String ENDPOINT_PROPERTY_NAME_PROPERTY = DSPACE_SCHEMA + "name";
    String ENDPOINT_PROPERTY_VALUE_PROPERTY = DSPACE_SCHEMA + "value";
}
