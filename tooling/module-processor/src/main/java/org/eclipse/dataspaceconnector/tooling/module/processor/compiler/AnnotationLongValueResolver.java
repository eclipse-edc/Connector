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

import javax.lang.model.util.SimpleAnnotationValueVisitor9;

/**
 * Returns the value of an annotation attribute as a long.
 */
class AnnotationLongValueResolver extends SimpleAnnotationValueVisitor9<Long, Void> {

    @Override
    public Long visitLong(long i, Void unused) {
        return super.visitLong(i, unused);
    }
}
