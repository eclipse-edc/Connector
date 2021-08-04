/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.transfer.nifi;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class NifiTransferException extends EdcException {
    public NifiTransferException(String message) {
        super(message);
    }
}
