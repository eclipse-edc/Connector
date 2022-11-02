/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

public class TestMapping extends TranslationMapping {
    public TestMapping() {
        add("field1", "edc_field_1");
        add("description", "edc_description");
        add("fooBar", "edc_foo_bar");
        add("complex", new ComplexMapping());

    }

    public static class ComplexMapping extends TranslationMapping {
        public ComplexMapping() {
            add("field2", "edc_field_2");
        }
    }
}
