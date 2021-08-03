/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.transfer.nifi;

import org.eclipse.edc.spi.EdcException;

public class NifiTransferException extends EdcException {
    public NifiTransferException(String message) {
        super(message);
    }
}
