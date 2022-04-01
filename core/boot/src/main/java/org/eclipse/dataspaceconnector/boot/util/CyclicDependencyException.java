/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.boot.util;

import org.eclipse.dataspaceconnector.spi.EdcException;

import static java.lang.String.format;

/**
 * Denotes a cyclic dependency was encountered during runtime boot.
 */
public class CyclicDependencyException extends EdcException {

    CyclicDependencyException(Object item) {
        super(format("Cyclic extension dependency for [%s]", item));
    }

    CyclicDependencyException(Object item, CyclicDependencyException e) {
        super(format("Cyclic extension dependency for [%s]", item), e);
    }
}
