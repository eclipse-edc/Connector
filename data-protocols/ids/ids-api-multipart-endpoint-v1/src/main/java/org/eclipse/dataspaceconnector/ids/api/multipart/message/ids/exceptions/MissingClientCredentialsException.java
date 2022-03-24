/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.message.ids.exceptions;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.List;

/**
 * The {@link  MissingClientCredentialsException} is thrown when there is a problem obtaining client credentials.
 */
public class MissingClientCredentialsException extends EdcException {
    public MissingClientCredentialsException(List<String> messages) {
        super(String.format("Missing client credentials. Problems: %s", String.join(", ", messages)));
    }
}
