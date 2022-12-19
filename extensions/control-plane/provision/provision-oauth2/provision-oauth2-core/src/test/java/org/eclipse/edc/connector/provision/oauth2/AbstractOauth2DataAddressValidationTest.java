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

package org.eclipse.edc.connector.provision.oauth2;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractOauth2DataAddressValidationTest {

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(DataAddressProvider.class)
    public void validate(String name, DataAddress address, boolean expected) {
        var isValid = test(address);

        assertThat(isValid).isEqualTo(expected);
    }

    protected abstract boolean test(DataAddress address);

    public static final class DataAddressProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            var addressWithPrivateKeyName = HttpDataAddress.Builder.newInstance()
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.PRIVATE_KEY_NAME, "aPrivateKeyName")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var addressWithSharedSecret = HttpDataAddress.Builder.newInstance()
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var missingTokenUrl = HttpDataAddress.Builder.newInstance()
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                    .build();

            var missingClientId = HttpDataAddress.Builder.newInstance()
                    .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            var missingPrivateKeyOrSharedSecret = HttpDataAddress.Builder.newInstance()
                    .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                    .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                    .build();

            return Stream.of(
                    Arguments.of("OK WITH PRIVATE KEY NAME", addressWithPrivateKeyName, true),
                    Arguments.of("OK WITH SHARED SECRET", addressWithSharedSecret, true),
                    Arguments.of("KO NO TOKEN URL", missingTokenUrl, false),
                    Arguments.of("KO NO CLIENT ID", missingClientId, false),
                    Arguments.of("KO NO PRIVATE KEY NAME OR SHARED SECRET", missingPrivateKeyOrSharedSecret, false)
            );
        }
    }
}
