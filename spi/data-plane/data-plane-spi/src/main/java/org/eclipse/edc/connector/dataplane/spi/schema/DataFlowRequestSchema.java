/*
 *  Copyright (c) 2020, 2021 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.edc.connector.dataplane.spi.schema;

import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * Schema of {@link DataFlowStartMessage} properties.
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
