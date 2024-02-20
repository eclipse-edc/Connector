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

package org.eclipse.edc.protocol.dsp.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for version request.
 */
public interface DspVersionPropertyAndTypeNames {

    String DSPACE_PROPERTY_PROTOCOL_VERSIONS = DSPACE_SCHEMA + "protocolVersions";

    String DSPACE_TYPE_VERSIONS_ERROR = DSPACE_SCHEMA + "VersionsError";
    String DSPACE_PROPERTY_VERSION = DSPACE_SCHEMA + "version";
    String DSPACE_PROPERTY_PATH = DSPACE_SCHEMA + "path";

}
