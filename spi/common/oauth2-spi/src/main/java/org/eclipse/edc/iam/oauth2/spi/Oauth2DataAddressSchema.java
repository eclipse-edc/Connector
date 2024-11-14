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

package org.eclipse.edc.iam.oauth2.spi;

public interface Oauth2DataAddressSchema {
    String CLIENT_ID = "oauth2:clientId";
    String CLIENT_SECRET_KEY = "oauth2:clientSecretKey";
    String KEY_ID = "oauth2:kid";
    String TOKEN_URL = "oauth2:tokenUrl";
    String VALIDITY = "oauth2:validity";
    String PRIVATE_KEY_NAME = "oauth2:privateKeyName";
    String SCOPE = "oauth2:scope";
}
