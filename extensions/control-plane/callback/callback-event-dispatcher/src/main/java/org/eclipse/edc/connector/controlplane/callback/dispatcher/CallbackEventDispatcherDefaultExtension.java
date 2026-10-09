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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.connector.controlplane.services.spi.callback.ParticipantCallbackResolver;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantContextConfigValidatorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.List;

@Extension(value = CallbackEventDispatcherDefaultExtension.NAME)
public class CallbackEventDispatcherDefaultExtension implements ServiceExtension {

    public static final String NAME = "Callback event dispatcher default services";

    @Inject
    EdcHttpClient edcHttpClient;
    @Inject
    TypeManager typeManager;
    @Inject
    Vault vault;
    @Inject(required = false)
    ParticipantContextConfig participantContextConfig;
    @Inject(required = false)
    ParticipantContextConfigValidatorRegistry configValidatorRegistry;
    @Inject
    Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (configValidatorRegistry != null) {
            configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(ParticipantContextConfigCallbackResolver.CALLBACKS_CONFIG_KEY)
                    .description("JSON array of the callback addresses notified of the participant context events")
                    .validator(ParticipantContextConfigCallbackResolver.callbacksValidator(typeManager::getMapper))
                    .build());
        }
    }

    @Provider(isDefault = true)
    public CallbackClient callbackClient() {
        return new CallbackHttpClient(edcHttpClient, typeManager.getMapper(), vault);
    }

    @Provider(isDefault = true)
    public ParticipantCallbackResolver participantCallbackResolver() {
        if (participantContextConfig == null) {
            return (participantContextId, eventName) -> List.of();
        }
        return new ParticipantContextConfigCallbackResolver(participantContextConfig, typeManager::getMapper, monitor);
    }
}
