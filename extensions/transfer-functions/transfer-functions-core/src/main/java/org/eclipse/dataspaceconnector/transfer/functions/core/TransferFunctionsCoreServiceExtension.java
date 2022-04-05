/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.functions.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.http.HttpDataFlowConfiguration;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.http.HttpDataFlowController;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.http.HttpStatusChecker;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.http.TransferFunctionInterceptorRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

/**
 * Bootstraps the transfer functions extension.
 */
@Provides(TransferFunction.class)
public class TransferFunctionsCoreServiceExtension implements ServiceExtension {

    @EdcSetting
    static final String ENABLED_PROTOCOLS_KEY = "edc.transfer.functions.enabled.protocols";

    @EdcSetting
    static final String TRANSFER_URL_KEY = "edc.transfer.functions.transfer.endpoint";

    @EdcSetting
    static final String CHECK_URL_KEY = "edc.transfer.functions.check.endpoint";

    private static final String DEFAULT_LOCAL_TRANSFER_URL = "http://localhost:9090/transfer";
    private static final String DEFAULT_LOCAL_CHECK_URL = "http://localhost:9090/checker";

    protected Monitor monitor;

    @Inject
    protected DataAddressResolver addressResolver;

    private Set<String> protocols;

    @Override
    public String name() {
        return "Transfer Functions Core";
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        protocols = getSupportedProtocols(context);

        initializeHttpFunctions(context);
    }

    private void initializeHttpFunctions(ServiceExtensionContext context) {
        var httpClient = createHttpClient(context);
        context.registerService(TransferFunctionInterceptorRegistry.class, httpClient::addInterceptor);

        var typeManager = context.getTypeManager();
        var transferEndpoint = context.getSetting(TRANSFER_URL_KEY, DEFAULT_LOCAL_TRANSFER_URL);
        var checkEndpoint = context.getSetting(CHECK_URL_KEY, DEFAULT_LOCAL_CHECK_URL);
        var configuration = HttpDataFlowConfiguration.Builder.newInstance()
                .transferEndpoint(transferEndpoint)
                .checkEndpoint(checkEndpoint)
                .clientSupplier(httpClient::build)
                .protocols(protocols)
                .typeManager(typeManager)
                .monitor(monitor)
                .build();

        var flowController = new HttpDataFlowController(configuration, addressResolver);
        var flowManager = context.getService(DataFlowManager.class);
        flowManager.register(flowController);

        var statusChecker = new HttpStatusChecker(configuration);
        var statusCheckerRegistry = context.getService(StatusCheckerRegistry.class);
        protocols.forEach(protocol -> statusCheckerRegistry.register(protocol, statusChecker));

        monitor.info("HTTP transfer functions are enabled");
    }

    /**
     * Parses the protocols supported by the configured transfer function.
     */
    @NotNull
    private Set<String> getSupportedProtocols(ServiceExtensionContext context) {
        var protocolsString = context.getSetting(ENABLED_PROTOCOLS_KEY, null);
        if (protocolsString == null) {
            monitor.info(format("No protocol is enabled for the Transfer Functions extension. One or more protocols can be enabled using the %s key. " +
                    "The extension will be disabled.", ENABLED_PROTOCOLS_KEY));
            return emptySet();
        } else {
            return new HashSet<>(asList(protocolsString.split(",")));
        }
    }

    /**
     * Creates an HTTP client. Note that this extension copies the default runtime HTTP client since this extension allows custom interceptors to be added.
     */
    private OkHttpClient.Builder createHttpClient(ServiceExtensionContext context) {
        var defaultClient = context.getService(OkHttpClient.class);
        return defaultClient.newBuilder();
    }

}
