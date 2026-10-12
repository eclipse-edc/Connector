/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.contract.spi.policy;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.jetbrains.annotations.Nullable;

/**
 * Policy context that carries the {@link Asset} the policy is evaluated for.
 */
public interface AssetPolicyContext extends PolicyContext {

    /**
     * The asset.
     *
     * @return the asset, null if not available.
     */
    @Nullable
    Asset asset();

}
