/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.stream.Stream;

class IdsIdParserTest {
    private static final String[] ILLEGAL_IDS = {
            null,
            "urn:test:12345asdasd",
            "https://example.com"
    };

    private static final String[] LEGAL_IDS = {
            "urn:artifact:artifact_id12345",
            "urn:artifact:https://example.com/catalog1/artifact/abc",
            "urn:catalog:catalog_id12345",
            "urn:catalog:https://example.com/catalog1",
            "urn:connector:connector_id12345",
            "urn:connector:http://example.com",
            "urn:constraint:constraint_id12345",
            "urn:contract:contract_id12345",
            "urn:contract:ctr1234",
            "urn:contractoffer:contractoffer_id12345",
            "urn:contractoffer:ctro1234",
            "urn:mediatype:application/json",
            "urn:mediatype:mediatype_id12345",
            "urn:message:message_id12345",
            "urn:participant:participant_id12345",
            "urn:permission:permission_id12345",
            "urn:prohibition:prohibition_id12345",
            "urn:representation:https://example.com/catalog1/artifact/abc/repr.json",
            "urn:representation:representation_id12345",
            "urn:resource:https://example.com/catalog1/artifact/abc/repr/resource.json",
            "urn:resource:resource_id12345"
    };

    @ParameterizedTest(name = "[index] parse legal id '{0}'")
    @ArgumentsSource(LegalIdsArgumentsProvider.class)
    void parseLegal(String string) {
        IdsId result = IdsIdParser.parse(string);
        Assertions.assertNotNull(result);
    }

    @ParameterizedTest(name = "[index] parse illegal id '{0}'")
    @ArgumentsSource(IllegalIdsArgumentsProvider.class)
    void parseIllegal(String string) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> IdsIdParser.parse(string));
    }

    static class IllegalIdsArgumentsProvider implements ArgumentsProvider {
        IllegalIdsArgumentsProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(ILLEGAL_IDS)
                    .map(Arguments::of);
        }
    }

    static class LegalIdsArgumentsProvider implements ArgumentsProvider {
        LegalIdsArgumentsProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(LEGAL_IDS)
                    .map(Arguments::of);
        }
    }
}
