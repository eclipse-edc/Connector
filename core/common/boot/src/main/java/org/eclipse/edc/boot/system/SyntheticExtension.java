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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

/**
 * This extension is solely intended to declare outgoing dependencies, i.e. it {@link Provides} special services and it should not go through the usual service loader mechanism.
 * It must also not declare any incoming dependencies (via {@link org.eclipse.edc.runtime.metamodel.annotation.Inject} or {@link org.eclipse.edc.runtime.metamodel.annotation.Requires}).
 */
@Provides({ Monitor.class, TypeManager.class, Telemetry.class, Clock.class })
class SyntheticExtension implements org.eclipse.edc.spi.system.ServiceExtension {
}
