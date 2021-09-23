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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.http.HttpFlowControllerConfiguration;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.http.HttpFunctionDataFlowController;
import org.eclipse.dataspaceconnector.transfer.functions.core.flow.local.LocalFunctionDataFlowController;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.http.TransferFunctionInterceptorRegistry;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.local.LocalTransferFunction;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Bootstraps the transfer functions extension. Two configurations are supported, as determined by the {@link #TRANSFER_TYPE} setting:
 *
 * <ol>
 * <li>HTTP - The extension delegates to an HTTP endpoint to initiate and manage data transfers.</li>
 * <li>LOCAL - The extension delegates to an-process {@link LocalTransferFunction} to initiate and manage data transfers.</li>
 * </ol>
 */
public class TransferFunctionsCoreServiceExtension implements ServiceExtension {

    @EdcSetting
    static final String ENABLED_PROTOCOLS_KEY = "edc.transfer.functions.enabled.protocols";

    @EdcSetting
    static final String DEFAULT_TIMEOUT_KEY = "edc.transfer.functions.timeout";

    @EdcSetting
    static final String FUNCTION_URL_KEY = "edc.transfer.functions.url";

    @EdcSetting
    static final String TRANSFER_TYPE = "edc.transfer.functions.type";

    private static final String DEFAULT_TIMEOUT = "30";

    private static final String DEFAULT_LOCAL_URL = "http://localhost:9090";

    private Monitor monitor;

    private boolean localTransfer;
    private Set<String> protocols;
    private ServiceExtensionContext context;

    @Override
    public Set<String> provides() {
        return Set.of("dataspaceconnector:transfer-function");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        monitor = context.getMonitor();

        protocols = getSupportedProtocols(context);

        var transferType = context.getSetting(TRANSFER_TYPE, "HTTP");
        localTransfer = "LOCAL".equalsIgnoreCase(transferType);

        if (localTransfer) {
            monitor.info("Local transfer functions are enabled");
        } else {
            initializeHttpFunctions(context);
        }

        monitor.info("Initialized Transfer Functions Core extension");
    }

    @Override
    public void start() {
        if (localTransfer) {
            var localTransferFunction = context.getService(LocalTransferFunction.class, true);
            if (localTransferFunction == null) {
                monitor.severe("Local transfer functions are configured. An implementation of "
                        + LocalTransferFunction.class.getName() + " must be provided. Transfer functions are disabled.");
                return;
            }
            var flowController = new LocalFunctionDataFlowController(protocols, localTransferFunction);
            var flowManager = context.getService(DataFlowManager.class);
            flowManager.register(flowController);
        }
        monitor.info("Started Transfer Functions Core extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Transfer Functions Core extension");
    }

    private void initializeHttpFunctions(ServiceExtensionContext context) {
        var httpClient = createHttpClient(context);
        context.registerService(TransferFunctionInterceptorRegistry.class, httpClient::addInterceptor);

        var typeManager = context.getTypeManager();
        var url = context.getSetting(FUNCTION_URL_KEY, DEFAULT_LOCAL_URL);
        var configuration = HttpFlowControllerConfiguration.Builder.newInstance()
                .url(url)
                .clientSupplier(httpClient::build)
                .protocols(protocols)
                .typeManager(typeManager)
                .monitor(monitor)
                .build();

        var flowController = new HttpFunctionDataFlowController(configuration);
        var flowManager = context.getService(DataFlowManager.class);
        flowManager.register(flowController);
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
     * Creates an HTTP client. Note that this extension does not use the default runtime HTTP client since this extension allows custom interceptors to be added.
     */
    private OkHttpClient.Builder createHttpClient(ServiceExtensionContext context) {
        var defaultTimeout = parseInt(context.getSetting(DEFAULT_TIMEOUT_KEY, DEFAULT_TIMEOUT));
        return new OkHttpClient.Builder().connectTimeout(defaultTimeout, SECONDS).readTimeout(defaultTimeout, SECONDS);
    }


}
