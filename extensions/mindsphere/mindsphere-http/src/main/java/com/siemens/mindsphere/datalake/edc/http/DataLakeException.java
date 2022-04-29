/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import java.io.IOException;

public class DataLakeException extends IOException {
    public DataLakeException(String message) {
        super(message);
    }

    public DataLakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
