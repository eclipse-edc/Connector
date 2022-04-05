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

package org.eclipse.dataspaceconnector.iam.did.crypto;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class CryptoException extends EdcException {
    public CryptoException(Exception inner) {
        super(inner);
    }

    public CryptoException() {
        super("Cyptographic Exception");
    }

    public CryptoException(String s) {
        super(s);
    }
}
