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

package org.eclipse.edc.connector.core.store;

import java.util.List;

public class TestObject {
    private String value;
    private TestEnum enumValue;
    private Integer intValue;
    private List<NestedObject> list;

    public TestObject(String value) {
        this(value, null);
    }

    public TestObject(int value) {
        this.intValue = value;
    }

    public TestObject(List<NestedObject> list) {
        this.list = list;
    }

    public TestObject(String value, TestEnum enumValue) {
        this.value = value;
        this.enumValue = enumValue;
    }

    @Override
    public String toString() {
        return "TestObject{" +
                "value='" + value + '\'' +
                ", enumValue=" + enumValue +
                ", intValue=" + intValue +
                ", list=" + list +
                '}';
    }

    public enum TestEnum {
        ENTRY1, ENTRY2
    }

    record NestedObject(String value) {
    }

    record StringTestObject(List<String> values) {

    }

}
