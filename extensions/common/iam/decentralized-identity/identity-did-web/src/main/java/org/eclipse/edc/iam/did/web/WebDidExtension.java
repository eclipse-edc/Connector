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

package org.eclipse.edc.iam.did.web;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.did.web.resolution.WebDidResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.util.string.StringUtils;

import java.time.Duration;


/**
 * Initializes support for resolving Web DIDs.
 */
@Extension(value = WebDidExtension.NAME)
public class WebDidExtension implements ServiceExtension {
    public static final String NAME = "Web DID";
    /**
     * If set, the resolver will use the endpoint to resolve DIDs using DNS over HTTPS.
     */
    @Setting
    public static final String DNS_OVER_HTTPS = "edc.webdid.doh.url";
    /**
     * Set to {@code false} to create DID URLs with {@code http} instead of {@code https} scheme.
     * Defaults to {@code true}.
     * <p>
     * This setting can be used by EDC downstream projects, e.g. for testing with docker compose
     */
    @Setting
    private static final String USE_HTTPS_SCHEME = "edc.iam.did.web.use.https";
    // DID documents are resolved while an incoming request is being verified: a slow or unresponsive host must not hold
    // the request for long. The verification fails as retryable, so the counter-party can send the request again.
    @Setting(
            key = "edc.iam.did.web.timeout.call",
            description = "Maximum duration of a single DID document request, including connecting and reading the response, in ISO-8601 duration format",
            defaultValue = WebDidResolver.DEFAULT_CALL_TIMEOUT)
    private Duration callTimeout;

    @Setting(
            key = "edc.iam.did.web.retries.max",
            description = "Maximum number of retries of a failed DID document request (connection failure or server error)",
            defaultValue = WebDidResolver.DEFAULT_MAX_RETRIES + "",
            min = 0)
    private int maxRetries;

    @Setting(
            key = "edc.iam.did.web.response.size.max",
            description = "Maximum size of a DID document, in bytes",
            defaultValue = WebDidResolver.DEFAULT_MAX_RESPONSE_BYTES + "",
            min = 1)
    private long maxResponseBytes;

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper();
        var monitor = context.getMonitor();
        var useHttpsScheme = context.getSetting(USE_HTTPS_SCHEME, true);

        var httpClient = getHttpClient(context);

        var resolver = new WebDidResolver(httpClient, useHttpsScheme, mapper, monitor, maxResponseBytes);

        resolverRegistry.register(resolver);
    }

    private EdcHttpClient getHttpClient(ServiceExtensionContext context) {
        var dnsServer = context.getSetting(DNS_OVER_HTTPS, null);

        // use DNS over HTTPS for name lookups if configured
        var client = StringUtils.isNullOrEmpty(dnsServer) ? httpClient : httpClient.withDns(dnsServer);
        return client.withCallTimeout(callTimeout).withMaxRetries(maxRetries);
    }

}
