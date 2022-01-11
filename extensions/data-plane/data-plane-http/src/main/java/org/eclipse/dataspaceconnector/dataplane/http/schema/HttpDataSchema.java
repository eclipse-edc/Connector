/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.http.schema;

/**
 * Defines the data address attributes used by the HTTP data plane extension.
 *
 * TODO discuss whether we need the Schema package at the SPI level anymore or this can be replaced by an annotation.
 */
public interface HttpDataSchema {

    /**
     * The HTTP transfer type.
     */
    String TYPE = "HttpData";

    /**
     * The source or destination endpoint.
     */
    String ENDPOINT = "endpoint";

    /**
     * The name associated with the HTTP data, typically a filename.
     */
    String NAME = "name";
}
