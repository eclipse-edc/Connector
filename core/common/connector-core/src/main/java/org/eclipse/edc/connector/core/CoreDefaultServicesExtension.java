/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.core.agent.NoOpParticipantIdMapper;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.Collections;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link CoreServicesExtension}
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public ControlClientAuthenticationProvider controlClientAuthenticationProvider() {
        return Collections::emptyMap;
    }

    @Provider(isDefault = true)
    public ParticipantIdMapper participantIdMapper() {
        return new NoOpParticipantIdMapper();
    }

}
