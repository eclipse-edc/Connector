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

import org.eclipse.edc.spi.result.Result;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Representation of <a href="https://www.w3.org/TR/2023/WD-vc-status-list-20230427/#bitstring-encoding">StatusList2021Credential#bitstring</a>
 */
public class BitString {

    private final boolean leftToRightIndexing;
    private final byte[] bits;
    private final int bitsPerByte = 8;

    private BitString(byte[] bits, boolean leftToRightIndexing) {
        this.bits = bits;
        this.leftToRightIndexing = leftToRightIndexing;
    }

    /**
     * Checks if the bit at the input index is `1`. The input index should be in the
     * bound (0-bitstring_length - 1). The default bit order is left to right, which means
     * that for a byte 00000001, the bit value of that first (zeroth) index is `0` and
     * the last index (seventh) is `1`
     *
     * @param idx The bit index to check
     * @return True if `1`, false otherwise
     */
    public boolean get(int idx) {

        if (idx < 0 || idx >= length()) {
            throw new IllegalArgumentException("Index out of range 0-%s".formatted(length()));
        }
        var byteIdx = idx / bitsPerByte;
        var bitIdx = idx % bitsPerByte;
        var shift = leftToRightIndexing ? (7 - bitIdx) : bitIdx;
        return (bits[byteIdx] & (1L << shift)) != 0;
    }

    public int length() {
        return bits.length * bitsPerByte;
    }

    /**
     * Parser configuration for {@link BitString}
     */
    public static final class Parser {
        private boolean leftToRightIndexing = true;
        private Base64.Decoder decoder = Base64.getDecoder();

        private Parser() {
        }

        public static Parser newInstance() {
            return new Parser();
        }

        public Parser leftToRightIndexing(boolean leftToRightIndexing) {
            this.leftToRightIndexing = leftToRightIndexing;
            return this;
        }

        public Parser decoder(Base64.Decoder decoder) {
            this.decoder = decoder;
            return this;
        }

        public Result<BitString> parse(String encodedList) {
            return Result.ofThrowable(() -> decoder.decode(encodedList))
                    .compose(this::unGzip)
                    .map(bytes -> new BitString(bytes, leftToRightIndexing));
        }

        private Result<byte[]> unGzip(byte[] bytes) {
            try (var inputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                try (var outputStream = new ByteArrayOutputStream()) {
                    inputStream.transferTo(outputStream);
                    return Result.success(outputStream.toByteArray());
                }
            } catch (IOException e) {
                return Result.failure("Failed to ungzip encoded list: %s".formatted(e.getMessage()));
            }
        }
    }
}
