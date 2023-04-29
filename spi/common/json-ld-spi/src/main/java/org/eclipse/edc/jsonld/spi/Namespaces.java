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

package org.eclipse.edc.jsonld.spi;

/**
 * Well-known schema namespace definitions.
 */
public interface Namespaces {

    String DCAT_PREFIX = "dcat";
    String DCAT_SCHEMA = "https://www.w3.org/ns/dcat/";

    // ref: https://www.w3.org/ns/odrl.jsonld
    String ODRL_PREFIX = "odrl";
    String ODRL_SCHEMA = "http://www.w3.org/ns/odrl/2/";

    String DCT_PREFIX = "dct";
    String DCT_SCHEMA = "https://purl.org/dc/terms/";

    String DSPACE_PREFIX = "dspace";
    String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/"; // TODO to be defined
}
