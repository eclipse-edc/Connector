/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.spi.schema;

/**
 * Schema of {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest} properties.
 */
public interface DataFlowRequestSchema {

    /**
     * The method to be used when calling the data source (optional).
     */
    String METHOD = "method";

    /**
     * The query parameters to be used when calling the data source (optional).
     */
    String QUERY_PARAMS = "queryParams";

    /**
     * The path segments to be used when calling the data source (optional).
     */
    String PATH = "pathSegments";

    /**
     * Media-type of the request body.
     */
    String MEDIA_TYPE = "mediaType";

    /**
     * The request body.
     */
    String BODY = "body";
}
