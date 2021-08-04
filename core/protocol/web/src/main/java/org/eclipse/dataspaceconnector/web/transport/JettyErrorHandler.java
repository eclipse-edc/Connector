/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.web.transport;

import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes errors as JSON.
 */
class JettyErrorHandler extends ErrorHandler {

    @Override
    protected void handleErrorPage(jakarta.servlet.http.HttpServletRequest request, Writer writer, int code, String message) throws IOException {
        writer.write("{ error: '" + code + "'}");
    }

}
