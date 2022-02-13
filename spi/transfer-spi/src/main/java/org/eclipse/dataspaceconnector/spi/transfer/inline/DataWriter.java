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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.inline;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.InputStream;

/**
 * A system responsible for writer data from to a destination (e.g. Azure blob storage,...)
 */
public interface DataWriter {
    boolean canHandle(String type);

    Result<Void> write(DataAddress destination, String name, InputStream data, String secretToken);
}
