package org.eclipse.dataspaceconnector.ids;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.stream.Stream;

class IdsIdTest {
    private static final String[] ILLEGAL_IDS = {
            null,
            "urn:test:12345asdasd",
            "https://example.com"
    };

    private static final String[] LEGAL_IDS = {
            "urn:contract:ctr1234",
            "urn:contractoffer:ctro1234",
            "urn:connector:http://example.com",
            "urn:catalog:https://example.com/catalog1",
            "urn:artifact:https://example.com/catalog1/artifact/abc",
            "urn:representation:https://example.com/catalog1/artifact/abc/repr.json",
            "urn:resource:https://example.com/catalog1/artifact/abc/repr/resource.json",
            "urn:mediatype:application/json"
    };

    @ParameterizedTest(name = "{index} {0} is 30 days long")
    @ArgumentsSource(LegalIdsArgumentsProvider.class)
    void parseLegal(String string) {
        IdsId result = IdsId.parse(string);
        Assertions.assertNotNull(result);
    }

    @ParameterizedTest
    @ArgumentsSource(IllegalIdsArgumentsProvider.class)
    void parseIllegal(String string) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> IdsId.parse(string));
    }

    static class IllegalIdsArgumentsProvider implements ArgumentsProvider {
        public IllegalIdsArgumentsProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(ILLEGAL_IDS)
                    .map(Arguments::of);
        }
    }

    static class LegalIdsArgumentsProvider implements ArgumentsProvider {
        public LegalIdsArgumentsProvider() {
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(LEGAL_IDS)
                    .map(Arguments::of);
        }
    }
}
