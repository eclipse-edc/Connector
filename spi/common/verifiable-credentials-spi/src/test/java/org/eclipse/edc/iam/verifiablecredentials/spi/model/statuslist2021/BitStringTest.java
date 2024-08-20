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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist2021;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BitStringTest {

    private static Base64.Decoder getDecoder(Format format) {
        return switch (format) {
            case Base64 -> Base64.getDecoder();
            case Base64Url -> Base64.getUrlDecoder();
        };
    }

    private static Base64.Encoder getEncoder(Format format) {
        return switch (format) {
            case Base64 -> Base64.getEncoder();
            case Base64Url -> Base64.getUrlEncoder();
        };
    }

    @Test
    void get() {

        var bitString = BitString.Builder.newInstance().build();

        assertThat(bitString.get(0)).isFalse();
        assertThat(bitString.get(50_000)).isFalse();

        bitString.set(0, true);
        bitString.set(50_000, true);

        assertThat(bitString.get(0)).isTrue();
        assertThat(bitString.get(50_000)).isTrue();

        bitString.set(0, false);
        bitString.set(50_000, false);

        assertThat(bitString.get(0)).isFalse();
        assertThat(bitString.get(50_000)).isFalse();
    }

    @Test
    void get_whenOutOfBound() {

        var bitString = BitString.Builder.newInstance().build();

        assertThatThrownBy(() -> bitString.get(200_000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bitString.get(-10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void set_whenOutOfBound() {

        var bitString = BitString.Builder.newInstance().build();

        assertThatThrownBy(() -> bitString.set(200_000, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bitString.set(-10, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_invalidSize() {
        assertThatThrownBy(() -> BitString.Builder.newInstance().size(10).build()).isInstanceOf(IllegalArgumentException.class);
    }

    enum Format {
        Base64,
        Base64Url
    }

    private static class ValidEncodedListProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    // Base64 decoder
                    Arguments.of("H4sIAAAAAAAAA+3BMQEAAADCoPVPbQsvoAAAAAAAAAAAAAAAAP4GcwM92tQwAAA=", 100_000, new int[]{}, Format.Base64, true),
                    Arguments.of("H4sIAAAAAAAAA+3BIQEAAAACIP+vcKozLEADAAAAAAAAAAAAAAAAAAAAvA0cOP65AEAAAA", 131072, new int[]{ 0, 2 }, Format.Base64, true),
                    Arguments.of("H4sIAAAAAAAAA+3OMQ0AAAgDsElHOh72EJJWQRMAAAAAAIDWXAcAAAAAAIDHFvRitn7UMAAA", 100_000, new int[]{ 50_000 }, Format.Base64, true),
                    Arguments.of("H4sIAAAAAAAAA+3BIQEAAAACIP1/2hkWoAEAAAAAAAAAAAAAAAAAAADeBjn7xTYAQAAA", 131072, new int[]{ 7 }, Format.Base64, true),
                    // Base64 URL decoder
                    Arguments.of("H4sIAAAAAAAAA-3BMQEAAADCoPVPbQsvoAAAAAAAAAAAAAAAAP4GcwM92tQwAAA", 100_000, new int[]{}, Format.Base64Url, true),
                    Arguments.of("H4sIAAAAAAAAA-3BIQEAAAACIP-vcKozLEADAAAAAAAAAAAAAAAAAAAAvA0cOP65AEAAAA", 131072, new int[]{ 0, 2 }, Format.Base64Url, true),
                    Arguments.of("H4sIAAAAAAAAA-3OMQ0AAAgDsElHOh72EJJWQRMAAAAAAIDWXAcAAAAAAIDHFvRitn7UMAAA", 100_000, new int[]{ 50_000 }, Format.Base64Url, true),
                    Arguments.of("H4sIAAAAAAAAA-3BIQEAAAACIP1_2hkWoAEAAAAAAAAAAAAAAAAAAADeBjn7xTYAQAAA", 131072, new int[]{ 7 }, Format.Base64Url, true),

                    // Left to right = false
                    Arguments.of("H4sIAAAAAAAA_-3AIQEAAAACIIv_LzvDAg0AAAAAAAAAAAAAAAAAAADwNgZXEi0AQAAA", 131072, new int[]{ 0, 2 }, Format.Base64Url, false)
            );
        }
    }

    @Nested
    class Parse {

        @ParameterizedTest
        @ArgumentsSource(ValidEncodedListProvider.class)
        void parse(String list, int size, int[] revoked, Format format, boolean leftToRightIndexing) {

            var result = BitString.Parser.newInstance().decoder(getDecoder(format)).leftToRightIndexing(leftToRightIndexing).parse(list);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).satisfies(bitString -> {
                assertThat(bitString.length()).isEqualTo(size);
                Arrays.stream(revoked).forEach((idx) -> assertThat(bitString.get(idx)).isTrue());
            });
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
    }

    @Nested
    class Write {

        @ParameterizedTest
        @ArgumentsSource(ValidEncodedListProvider.class)
        void write(String list, int size, int[] revoked, Format format, boolean leftToRightIndexing) {

            var bitString = BitString.Builder.newInstance().size(size).leftToRightIndexing(leftToRightIndexing).build();
            assertThat(bitString.length()).isEqualTo(size);

            Arrays.stream(revoked).forEach((idx) -> bitString.set(idx, true));
            Arrays.stream(revoked).forEach((idx) -> assertThat(bitString.get(idx)).isTrue());

            var result = BitString.Writer.newInstance().encoder(getEncoder(format)).write(bitString);

            var decoder = getDecoder(format);
            assertThat(result.succeeded()).isTrue();
            assertThat(decode(list, decoder)).isEqualTo(decode(result.getContent(), decoder));
        }

        private byte[] decode(String list, Base64.Decoder decoder) {
            return decompress(decoder.decode(list));
        }

        private byte[] decompress(byte[] bytes) {
            try (var inputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                try (var outputStream = new ByteArrayOutputStream()) {
                    inputStream.transferTo(outputStream);
                    return outputStream.toByteArray();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
