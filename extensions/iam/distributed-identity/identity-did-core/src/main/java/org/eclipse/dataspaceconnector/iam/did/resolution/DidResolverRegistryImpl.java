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
package org.eclipse.dataspaceconnector.iam.did.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation.
 */
public class DidResolverRegistryImpl implements DidResolverRegistry {
    private static final String DID = "did";
    private static final int DID_PREFIX = 0;
    private static final int DID_METHOD_NAME = 1;

    private Map<String, DidResolver> resolvers = new HashMap<>();

    @Override
    public void register(DidResolver resolver) {
        resolvers.put(resolver.getMethod(), resolver);
    }

    @Override
    public Result resolve(String didKey) {
        Objects.requireNonNull(didKey);
        // for the definition of DID syntax, .cf https://www.w3.org/TR/did-core/#did-syntax
        var tokens = didKey.split(":");
        if (tokens.length < 3) {
            return new Result("Invalid DID format. The DID must be in the form:  \"did:\" method-name \":\" method-specific-id");
        }
        if (!DID.equalsIgnoreCase(tokens[DID_PREFIX])) {
            return new Result("Invalid DID prefix");
        }
        var methodName = tokens[DID_METHOD_NAME];
        var resolver = resolvers.get(methodName);
        if (resolver == null) {
            return new Result("No resolver registered for DID Method: " + methodName);
        }
        var didDocument = resolver.resolve(didKey);
        return didDocument != null ? new Result(didDocument) : new Result("DID Document not found: " + didKey);
    }
}
