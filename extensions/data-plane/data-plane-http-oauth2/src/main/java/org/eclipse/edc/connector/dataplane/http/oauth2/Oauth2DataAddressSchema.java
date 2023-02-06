/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.http.oauth2;

public interface Oauth2DataAddressSchema {
    String CLIENT_ID = "oauth2:clientId";
    String CLIENT_SECRET_KEY = "oauth2:clientSecretKey";
    String TOKEN_URL = "oauth2:tokenUrl";
    String VALIDITY = "oauth2:validity";
    String PRIVATE_KEY_NAME = "oauth2:privateKeyName";
    String SCOPE = "oauth2:scope";

    /**
     * The client secret shouldn't be stored in the data address anymore, please store the key and then put the value
     * into the Vault
     *
     * @deprecated use CLIENT_SECRET_KEY instead
     */
    @Deprecated(since = "milestone8")
    String CLIENT_SECRET = "oauth2:clientSecret";
}
