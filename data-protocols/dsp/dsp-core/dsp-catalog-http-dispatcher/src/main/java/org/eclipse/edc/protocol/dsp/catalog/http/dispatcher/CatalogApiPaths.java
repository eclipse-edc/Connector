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

package org.eclipse.edc.protocol.dsp.catalog.http.dispatcher;

/**
 * API paths for catalog requests as defined in the dataspace protocol specification.
 */
public interface CatalogApiPaths {

    String BASE_PATH = "/catalog";
    String CATALOG_REQUEST = "/request";
    String DATASET_REQUEST = "/datasets";

}
