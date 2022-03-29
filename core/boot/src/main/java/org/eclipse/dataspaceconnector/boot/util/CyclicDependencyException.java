//
//  Code based on TopologicalSort from Eclipse Jetty licensed under Apache 2.0 (https://www.eclipse.org/jetty/).
//
//  Original license notice:
//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
/*
 *  Copyright (c) 2020, 1995-2021 Microsoft Corporation
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
