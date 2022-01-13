/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.extension.jetty;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes errors as JSON.
 */
class JettyErrorHandler extends ErrorHandler {

    @Override
    protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
        writer.write("{ error: '" + code + "'}");
    }

}
