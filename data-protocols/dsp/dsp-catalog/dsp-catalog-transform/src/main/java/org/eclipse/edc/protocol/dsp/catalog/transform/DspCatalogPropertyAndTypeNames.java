/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspCatalogPropertyAndTypeNames {
    
    String DSPACE_PREFIX = "dspace";
    String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/"; // TODO to be defined
    
    String DSPACE_CATALOG_REQUEST_TYPE = DSPACE_SCHEMA + "CatalogRequestMessage";
    String DSPACE_FILTER_PROPERTY = DSPACE_SCHEMA + "filter";
    
}
