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

import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleElementVisitor9;

/**
 * Resolves the type name of an element.
 */
class VariableTypeResolver extends SimpleElementVisitor9<String, Void> {

    @Override
    public String visitVariable(VariableElement e, Void unused) {
        return e.asType().toString();
    }
}
