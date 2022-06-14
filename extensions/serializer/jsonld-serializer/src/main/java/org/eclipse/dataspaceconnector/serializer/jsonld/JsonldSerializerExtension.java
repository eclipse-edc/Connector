/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.serializer.jsonld;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides a custom object mapper that serializes java objects to json-ld strings.
 */
public class JsonldSerializerExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return "JSON-LD Serializer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(JsonldSerializer.class, new JsonldSerializer(monitor));
        monitor.info(name() + " extension initialized");
    }
}
