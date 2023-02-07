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

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.function.Predicate;

import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_ID;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_SECRET;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.CLIENT_SECRET_KEY;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.PRIVATE_KEY_NAME;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.TOKEN_URL;

/**
 * Validates {@link DataAddress}, returns true if the Address has the fields needed for the OAuth2 provisioning
 */
public class Oauth2DataAddressValidator implements Predicate<DataAddress> {

    private final Predicate<DataAddress> isHttpDataType = dataAddress -> "HttpData".equals(dataAddress.getType());
    private final Predicate<DataAddress> hasClientId = dataAddress -> dataAddress.hasProperty(CLIENT_ID);
    private final Predicate<DataAddress> hasClientSecret = dataAddress -> dataAddress.hasProperty(CLIENT_SECRET);
    private final Predicate<DataAddress> hasClientSecretKey = dataAddress -> dataAddress.hasProperty(CLIENT_SECRET_KEY);
    private final Predicate<DataAddress> hasPrivateKeySecretName = dataAddress -> dataAddress.hasProperty(PRIVATE_KEY_NAME);
    private final Predicate<DataAddress> hasTokenUrl = dataAddress -> dataAddress.hasProperty(TOKEN_URL);
    private final Predicate<DataAddress> hasSecretInfo = hasClientSecretKey.or(hasClientSecret).or(hasPrivateKeySecretName);

    @Override
    public boolean test(DataAddress dataAddress) {
        return isHttpDataType
                .and(hasClientId)
                .and(hasTokenUrl)
                .and(hasSecretInfo)
                .test(dataAddress);
    }

}
