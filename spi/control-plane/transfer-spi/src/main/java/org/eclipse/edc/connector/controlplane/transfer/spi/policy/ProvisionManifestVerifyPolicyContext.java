/*
 *  Copyright (c) 2024 Cofinity-X
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

package org.eclipse.edc.connector.controlplane.transfer.spi.policy;

import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyScope;

/**
 * Policy Context for "provision.manifest.verify" scope
 */
public class ProvisionManifestVerifyPolicyContext extends PolicyContextImpl {

    @PolicyScope
    public static final String MANIFEST_VERIFICATION_SCOPE = "provision.manifest.verify";

    private final ResourceManifestContext resourceManifestContext;

    public ProvisionManifestVerifyPolicyContext(ResourceManifestContext resourceManifestContext) {
        this.resourceManifestContext = resourceManifestContext;
    }

    public ResourceManifestContext resourceManifestContext() {
        return resourceManifestContext;
    }

    @Override
    public String scope() {
        return MANIFEST_VERIFICATION_SCOPE;
    }
}
