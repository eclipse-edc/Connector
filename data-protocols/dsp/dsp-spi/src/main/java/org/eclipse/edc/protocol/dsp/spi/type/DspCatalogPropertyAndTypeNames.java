/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspCatalogPropertyAndTypeNames {

    String DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM = "CatalogRequestMessage";
    @Deprecated(since = "0.14.0")
    String DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_IRI = DSPACE_SCHEMA + DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
    String DSPACE_TYPE_CATALOG_ERROR_TERM = "CatalogError";
    @Deprecated(since = "0.14.0")
    String DSPACE_TYPE_CATALOG_ERROR_IRI = DSPACE_SCHEMA + DSPACE_TYPE_CATALOG_ERROR_TERM;
    String DSPACE_PROPERTY_FILTER_TERM = "filter";
    @Deprecated(since = "0.14.0")
    String DSPACE_PROPERTY_FILTER_IRI = DSPACE_SCHEMA + DSPACE_PROPERTY_FILTER_TERM;

}
