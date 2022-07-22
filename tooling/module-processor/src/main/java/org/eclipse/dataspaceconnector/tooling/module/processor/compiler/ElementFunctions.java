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

package org.eclipse.dataspaceconnector.tooling.module.processor.compiler;

import javax.lang.model.element.Element;

/**
 * Contains convenience methods for working with Java Compiler API.
 */
public class ElementFunctions {
    private static final VariableTypeResolver VARIABLE_TYPE_RESOLVER = new VariableTypeResolver();

    /**
     * Returns the type of the given element.
     */
    public static String typeFor(Element element) {
        return element.accept(VARIABLE_TYPE_RESOLVER, null);
    }

    private ElementFunctions() {
    }
}
