/*
 *  Copyright (c) 2023 NTT DATA Group Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       NTT DATA Group Corporation - initial implementation
 *
 */

package org.eclipse.edc.monitor.slf4j;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Adds bridge forwarding logs from slf4j API to Monitor.
 */
@Extension(value = Slf4jBridgeExtension.NAME)
public class Slf4jBridgeExtension implements ServiceExtension {
    public static final String NAME = "SLF4J-Monitor Bridge Extension";

    @Override
    public void initialize(ServiceExtensionContext context) {
        MonitorProvider.initialize(context);
    }
}
