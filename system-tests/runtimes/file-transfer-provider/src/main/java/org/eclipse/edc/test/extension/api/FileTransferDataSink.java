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

package org.eclipse.edc.test.extension.api;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

class FileTransferDataSink extends ParallelSink {
    private File file;

    @WithSpan
    @Override
    protected StreamResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            var fileName = part.name();
            try (var input = part.openStream()) {
                try (var output = new FileOutputStream(file)) {
                    try {
                        input.transferTo(output);
                    } catch (Exception e) {
                        return getTransferResult(e, "Error transferring file %s", fileName);
                    }
                } catch (Exception e) {
                    return getTransferResult(e, "Error creating file %s", fileName);
                }
            } catch (Exception e) {
                return getTransferResult(e, "Error reading file %s", fileName);
            }
        }
        return StreamResult.success();
    }

    private StreamResult<Void> getTransferResult(Exception e, String logMessage, Object... args) {
        var message = format(logMessage, args);
        monitor.severe(message, e);
        return StreamResult.error(message);
    }

    public static class Builder extends ParallelSink.Builder<Builder, FileTransferDataSink> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder file(File file) {
            sink.file = file;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.file, "file");
        }

        private Builder() {
            super(new FileTransferDataSink());
        }
    }
}
