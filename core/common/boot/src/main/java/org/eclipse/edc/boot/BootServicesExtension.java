/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.boot;

import org.eclipse.edc.boot.system.ExtensionLoader;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;


@BaseExtension
@Extension(value = BootServicesExtension.NAME)
public class BootServicesExtension implements ServiceExtension {

    public static final String NAME = "Boot Services";

    @Setting(value = "Configures the participant id this runtime is operating on behalf of")
    public static final String PARTICIPANT_ID = "edc.participant.id";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TypeManager typeManager() {
        return new TypeManager();
    }

    @Provider
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Provider
    public Telemetry telemetry() {
        return ExtensionLoader.loadTelemetry();
    }

}
