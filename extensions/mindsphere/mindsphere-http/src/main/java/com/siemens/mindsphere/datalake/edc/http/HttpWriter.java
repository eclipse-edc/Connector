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

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataWriter;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpWriter implements DataWriter {
    public HttpWriter(Monitor monitor) {
        this.monitor = monitor;
    }

    private Monitor monitor;

    @Override
    public boolean canHandle(String type) {
        return HttpSchema.TYPE.equals(type);
    }

    @Override
    public Result<Void> write(DataAddress destination, String name, InputStream data, String secretToken) {
        monitor.info("Writing to HTTP asset: " + name);
        final String urlString = destination.getProperty(HttpSchema.URL);
        if (urlString == null) {
            return Result.failure("No destination URL provided");
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("PUT");

            try (OutputStream outputStream = urlConnection.getOutputStream()) {
                final byte[] bytes = data.readAllBytes();
                outputStream.write(bytes);
            } catch (IOException e) {
                return Result.failure("Unable to write to the source: " + e.getMessage());
            }

            // check the HTTP response code to complete the upload.
            final int responseCode = urlConnection.getResponseCode();
            monitor.info("Response code: " + responseCode);
            return Result.success();
        } catch (MalformedURLException e) {
            return Result.failure("Malformed URL provided");
        } catch (IOException e) {
            return Result.failure("Unable to open connection to the source");
        }
    }
}
