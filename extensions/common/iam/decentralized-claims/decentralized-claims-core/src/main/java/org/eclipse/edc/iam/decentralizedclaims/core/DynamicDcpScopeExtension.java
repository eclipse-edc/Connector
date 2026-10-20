/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.iam.decentralizedclaims.core.scope.DcpScopeRegistryImpl;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.DynamicScopeExtractor;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.iam.decentralizedclaims.core.DynamicDcpScopeExtension.NAME;

@Extension(NAME)
public class DynamicDcpScopeExtension implements ServiceExtension {

    public static final String NAME = "DCP Dynamic Scope Extension";

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private DcpScopeStore scopeStore;


    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;

    private DcpScopeRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        scopeExtractorRegistry.registerScopeExtractor(new DynamicScopeExtractor(registry()));
    }

    @Provider
    public DcpScopeRegistry registry() {
        if (registry == null) {
            registry = new DcpScopeRegistryImpl(transactionContext, scopeStore);
        }
        return registry;
    }
}
