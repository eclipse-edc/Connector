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

package org.eclipse.dataspaceconnector.spi.system.injection;

import org.eclipse.dataspaceconnector.spi.EdcException;

/**
 * Raised when an error performing service injection is encountered.
 */
public class EdcInjectionException extends EdcException {
    public EdcInjectionException(String s) {
        super(s);
    }

    public EdcInjectionException(Throwable e) {
        super(e);
    }
}
