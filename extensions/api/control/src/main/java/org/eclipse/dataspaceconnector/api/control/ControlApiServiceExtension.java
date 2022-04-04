/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.function.Predicate;

public class ControlApiServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_API_CONTROL_AUTH_APIKEY_KEY = "edc.api.control.auth.apikey.key";
    public static final String EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT = "X-Api-Key";

    @EdcSetting
    public static final String EDC_API_CONTROL_AUTH_APIKEY_VALUE = "edc.api.control.auth.apikey.value";

    private Monitor monitor;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessManager transferProcessManager;
    @Inject
    private RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry;
    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    /*
     * Produces twelve characters long sequence in the ascii range of '!' (dec 33) to '~' (dec 126).
     *
     * @return sequence
     */
    private static String generateRandomString() {
        StringBuilder stringBuilder = new SecureRandom().ints('!', ((int) '~' + 1)).limit(12).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
        return stringBuilder.toString();
    }

    @Override
    public String name() {
        return "EDC Control API";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();


        webService.registerResource(new ClientController(transferProcessManager, consumerNegotiationManager, contractNegotiationStore));
        webService.registerResource(new ClientControlCatalogApiController(remoteMessageDispatcherRegistry));

        /*
         * Registers a API-Key authentication filter
         */
        HttpApiKeyAuthContainerRequestFilter httpApiKeyAuthContainerRequestFilter = new HttpApiKeyAuthContainerRequestFilter(resolveApiKeyHeaderName(serviceExtensionContext), resolveApiKeyHeaderValue(serviceExtensionContext),
                AuthenticationContainerRequestContextPredicate.INSTANCE);

        webService.registerResource(httpApiKeyAuthContainerRequestFilter);

        // contribute to the liveness probe
        var hcs = serviceExtensionContext.getService(HealthCheckService.class, true);
        if (hcs != null) {
            hcs.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Control API").build());
        }
    }

    private String resolveApiKeyHeaderName(@NotNull ServiceExtensionContext context) {
        String key = context.getSetting(EDC_API_CONTROL_AUTH_APIKEY_KEY, null);
        if (key == null) {
            key = EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT;
            monitor.warning(String.format("Settings: No setting found for key '%s'. Using default value '%s'", EDC_API_CONTROL_AUTH_APIKEY_KEY, EDC_API_CONTROL_AUTH_APIKEY_KEY_DEFAULT));
        }
        return key;
    }

    private String resolveApiKeyHeaderValue(@NotNull ServiceExtensionContext context) {
        String value = context.getSetting(EDC_API_CONTROL_AUTH_APIKEY_VALUE, null);
        if (value == null) {
            value = generateRandomString();
            monitor.warning(String.format("Settings: No setting found for key '%s'. Using random value '%s'", EDC_API_CONTROL_AUTH_APIKEY_VALUE, value));
        }
        return value;
    }

    private enum AuthenticationContainerRequestContextPredicate implements Predicate<ContainerRequestContext> {
        INSTANCE;

        @Override
        public boolean test(ContainerRequestContext containerRequestContext) {
            String path = containerRequestContext.getUriInfo().getPath();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            return path.startsWith("/control");
        }
    }
}
