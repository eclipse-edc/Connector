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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;

public class EnvelopePackerTest {

    @Test
    public void testPackingOfUuids() {
        UUID expected = UUID.randomUUID();

        String packed = EnvelopePacker.pack(expected);
        UUID unpacked = EnvelopePacker.unpack(packed);

        Assertions.assertEquals(expected, unpacked);
    }

    @Test
    public void testPackingOfString() {
        String expected = "Hello, World!";

        String packed = EnvelopePacker.pack(expected);
        String unpacked = EnvelopePacker.unpack(packed);

        Assertions.assertEquals(expected, unpacked);
    }

    @Test
    public void testPackingOfInnerClass() {
        Pojo expected = new Pojo("pojo", 1,
                new Pojo("pojo", 2, null));

        String packed = EnvelopePacker.pack(expected);
        Pojo unpacked = EnvelopePacker.unpack(packed);

        Assertions.assertEquals(expected, unpacked);
    }

    private static class Pojo {
        private String text;
        private int number;
        private Pojo obj;

        public Pojo() {
        }

        public Pojo(@NotNull String text, int number, Pojo obj) {
            this.text = text;
            this.number = number;
            this.obj = obj;
        }

        public String getText() {
            return text;
        }

        public int getNumber() {
            return number;
        }

        public Pojo getObj() {
            return obj;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public void setObj(Pojo obj) {
            this.obj = obj;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pojo pojo = (Pojo) o;
            return number == pojo.number && Objects.equals(text, pojo.text) && Objects.equals(obj, pojo.obj);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, number, obj);
        }
    }
}
