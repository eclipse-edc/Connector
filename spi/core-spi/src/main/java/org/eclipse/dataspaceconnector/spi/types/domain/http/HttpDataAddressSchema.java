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

package org.eclipse.dataspaceconnector.spi.types.domain.http;

/**
 * Defines the schema of a {@link org.eclipse.dataspaceconnector.spi.types.domain.DataAddress} representing a Http endpoint.
 */
public interface HttpDataAddressSchema {

    /**
     * The HTTP transfer type.
     */
    String TYPE = "HttpData";

    /**
     * The http endpoint.
     */
    String ENDPOINT = "endpoint";

    /**
     * The name associated with the HTTP data, typically a filename (optional).
     */
    String NAME = "name";

    /**
     * The authentication key property name (optional).
     */
    String AUTHENTICATION_KEY = "authKey";

    /**
     * The authentication code property name (optional).
     */
    String AUTHENTICATION_CODE = "authCode";

    /**
     * The name of the vault secret that is containing the authorization code (optional).
     */
    String SECRET_NAME = "secretName";

}
