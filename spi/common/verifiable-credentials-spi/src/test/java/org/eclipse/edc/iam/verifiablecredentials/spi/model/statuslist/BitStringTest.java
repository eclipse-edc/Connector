/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BitStringTest {

    @ParameterizedTest
    @ArgumentsSource(ValidEncodedListProvider.class)
    void parse(String list, int size, int[] revoked, Base64.Decoder decoder, boolean leftToRightIndexing) {

        var result = BitString.Parser.newInstance().decoder(decoder).leftToRightIndexing(leftToRightIndexing).parse(list);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).satisfies(bitString -> {
            assertThat(bitString.length()).isEqualTo(size);
            Arrays.stream(revoked).forEach((idx) -> assertThat(bitString.get(idx)).isTrue());
        });
    }

    @Test
    void get_whenOutOfBound() {

        var bitString = BitString.Parser.newInstance().parse("H4sIAAAAAAAAA+3BMQEAAADCoPVPbQsvoAAAAAAAAAAAAAAAAP4GcwM92tQwAAA=").getContent();

        assertThatThrownBy(() -> bitString.get(200_000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bitString.get(-10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_invalidBas64() {

        var result = BitString.Parser.newInstance().parse("invalid-");
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Illegal base64 character");
    }

    @Test
    void parse_invalidGzip() {

        var result = BitString.Parser.newInstance().parse("invalid/gzip");
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to ungzip encoded list: Not in GZIP format");
    }

    private static class ValidEncodedListProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    // Base64 decoder
                    Arguments.of("H4sIAAAAAAAAA+3BMQEAAADCoPVPbQsvoAAAAAAAAAAAAAAAAP4GcwM92tQwAAA=", 100_000, new int[]{}, Base64.getDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA+3BIQEAAAACIP+vcKozLEADAAAAAAAAAAAAAAAAAAAAvA0cOP65AEAAAA", 131072, new int[]{ 0, 2 }, Base64.getDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA+3OMQ0AAAgDsElHOh72EJJWQRMAAAAAAIDWXAcAAAAAAIDHFvRitn7UMAAA", 100_000, new int[]{ 50_000 }, Base64.getDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA+3BIQEAAAACIP1/2hkWoAEAAAAAAAAAAAAAAAAAAADeBjn7xTYAQAAA", 131072, new int[]{ 7 }, Base64.getDecoder(), true),
                    // Base64 URL decoder
                    Arguments.of("H4sIAAAAAAAAA-3BMQEAAADCoPVPbQsvoAAAAAAAAAAAAAAAAP4GcwM92tQwAAA", 100_000, new int[]{}, Base64.getUrlDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA-3BIQEAAAACIP-vcKozLEADAAAAAAAAAAAAAAAAAAAAvA0cOP65AEAAAA", 131072, new int[]{ 0, 2 }, Base64.getUrlDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA-3OMQ0AAAgDsElHOh72EJJWQRMAAAAAAIDWXAcAAAAAAIDHFvRitn7UMAAA", 100_000, new int[]{ 50_000 }, Base64.getUrlDecoder(), true),
                    Arguments.of("H4sIAAAAAAAAA-3BIQEAAAACIP1_2hkWoAEAAAAAAAAAAAAAAAAAAADeBjn7xTYAQAAA", 131072, new int[]{ 7 }, Base64.getUrlDecoder(), true),

                    // Left to right = false
                    Arguments.of("H4sIAAAAAAAA_-3AIQEAAAACIIv_LzvDAg0AAAAAAAAAAAAAAAAAAADwNgZXEi0AQAAA", 131072, new int[]{ 0, 2 }, Base64.getUrlDecoder(), false)
            );
        }
    }
}
