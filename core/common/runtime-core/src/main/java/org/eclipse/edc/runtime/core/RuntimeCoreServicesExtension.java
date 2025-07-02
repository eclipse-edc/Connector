/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.runtime.core;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.core.command.CommandHandlerRegistryImpl;
import org.eclipse.edc.runtime.core.event.EventRouterImpl;
import org.eclipse.edc.runtime.core.message.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.edc.runtime.core.validator.JsonObjectValidatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.time.Clock;
import java.util.concurrent.Executors;

import static org.eclipse.edc.runtime.core.RuntimeDefaultCoreServicesExtension.NAME;

@Extension(NAME)
public class RuntimeCoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Runtime Core Services";
    private static final String DEFAULT_EDC_HOSTNAME = "localhost";

    @Setting(
            key = "edc.hostname",
            description = "Runtime hostname, which e.g. is used in referer urls",
            defaultValue = DEFAULT_EDC_HOSTNAME,
            warnOnMissingConfig = true
    )
    public String hostname;

    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private RetryPolicy<Response> retryPolicy;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TypeManager typeManager() {
        return new JacksonTypeManager();
    }

    @Provider
    public Hostname hostname(ServiceExtensionContext context) {
        return () -> hostname;
    }

    @Provider
    public EdcHttpClient edcHttpClient(ServiceExtensionContext context) {
        return new EdcHttpClientImpl(okHttpClient, retryPolicy, context.getMonitor());
    }

    @Provider
    public CommandHandlerRegistry commandHandlerRegistry() {
        return new CommandHandlerRegistryImpl();
    }

    @Provider
    public EventRouter eventRouter(ServiceExtensionContext context) {
        return new EventRouterImpl(context.getMonitor(), Executors.newFixedThreadPool(1), clock);
    }

    @Provider
    public TypeTransformerRegistry typeTransformerRegistry() {
        return new TypeTransformerRegistryImpl();
    }

    @Provider
    public JsonObjectValidatorRegistry jsonObjectValidator() {
        return new JsonObjectValidatorRegistryImpl();
    }

    @Provider
    public CriterionOperatorRegistry criterionOperatorRegistry() {
        return CriterionOperatorRegistryImpl.ofDefaults();
    }

    @Provider
    public RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry() {
        return new RemoteMessageDispatcherRegistryImpl();
    }

}
