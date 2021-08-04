/*
 *  Copyright (c) 2020, 1995-2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.util;

import org.eclipse.dataspaceconnector.spi.EdcException;

/**
 * Denotes a cyclic dependency was encountered during runtime boot.
 */
public class CyclicDependencyException extends EdcException {

    CyclicDependencyException(Object item) {
        super("Cyclic extension dependency for " + item);
    }

    CyclicDependencyException(Object item, CyclicDependencyException e) {
        super("Cyclic extension dependency for " + item, e);
    }
}
