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
package org.eclipse.dataspaceconnector.iam.did.credentials;

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsResult;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.HashMap;

/**
 * Implements a sample credentials validator that checks for signed registration credentials.
 */
public class IdentityHubCredentialsVerifier implements CredentialsVerifier {
    private final IdentityHubClient hubClient;
    private final Monitor monitor;
    private final String issuer;

    /**
     * Create a new credentials verifier that uses an Identity Hub
     *
     * @param hubClient an instance of a {@link IdentityHubClient}
     * @param monitor   a {@link Monitor}
     * @param issuer    a String identifying "this" connector. Here, the verifying connector's DID URL needs to be passed.
     */
    public IdentityHubCredentialsVerifier(IdentityHubClient hubClient, Monitor monitor, String issuer) {
        this.hubClient = hubClient;
        this.monitor = monitor;
        this.issuer = issuer;
    }

    @Override
    public CredentialsResult verifyCredentials(String hubBaseUrl, PublicKeyWrapper othersPublicKey) {
        monitor.debug("Starting credential verification against hub URL " + hubBaseUrl);

        var query = ObjectQuery.Builder.newInstance().context("ION Demo").type("RegistrationCredentials").build();
        var queryRequest = ObjectQueryRequest.Builder.newInstance().query(query).iss(issuer).aud("aud").sub("credentials").build();
        var credentials = hubClient.queryCredentials(queryRequest, hubBaseUrl, othersPublicKey);
        monitor.info(credentials.getResponse().size() + " credentials obtained");
        if (credentials.isError()) {
            return new CredentialsResult("Error resolving credentials");
        }

        // only support String credentials; filter out others
        var map = new HashMap<String, String>();
        credentials.getResponse().entrySet().stream().filter(entry -> entry.getValue() instanceof String).forEach(entry -> map.put(entry.getKey(), (String) entry.getValue()));
        return new CredentialsResult(map);
    }
}
