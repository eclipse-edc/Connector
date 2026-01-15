/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.test.runtime.signaling;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.edc.spi.monitor.Monitor;

@Path("/")
public class SourceDataController {
    private final Monitor monitor;

    public SourceDataController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    @Path("/source")
    public String dataSource() {
        monitor.info("Data requested");
        return "data";
    }
}
