/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.edc.iam.oauth2.spi;

import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2DataAddressValidatorTest {

    private final Oauth2DataAddressValidator validator = new Oauth2DataAddressValidator();

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(DataAddressProvider.class)
    public void validate(String name, DataAddress address, boolean expected) {
        var isValid = validator.test(address);

        assertThat(isValid).isEqualTo(expected);
    }

    public static final class DataAddressProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var addressWithPrivateKeyName = DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.PRIVATE_KEY_NAME, "aPrivateKeyName")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var addressWithSharedSecret = DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET_KEY, "aSecret")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var missingTokenUrl = DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET_KEY, "aSecret")
                    .build();

            var missingClientId = DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET_KEY, "secret-key")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var missingPrivateKeyOrSharedSecret = DataAddress.Builder.newInstance()
                    .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            return Stream.of(
                    Arguments.of("OK WITH PRIVATE KEY NAME", addressWithPrivateKeyName, true),
                    Arguments.of("OK WITH SHARED SECRET KEY", addressWithSharedSecret, true),
                    Arguments.of("KO NO TOKEN URL", missingTokenUrl, false),
                    Arguments.of("KO NO CLIENT ID", missingClientId, false),
                    Arguments.of("KO NO PRIVATE KEY NAME OR SHARED SECRET KEY", missingPrivateKeyOrSharedSecret, false)
            );
        }
    }
}
