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

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.AssetPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes the {@link Asset} of an {@link AssetPolicyContext} as {@code ctx.asset}, with its {@code id},
 * {@code participantContextId}, {@code properties} and {@code privateProperties}. Contexts that do not carry an asset
 * get no {@code asset} entry.
 */
public class AssetContextMapper implements CelContextMapper<PolicyContext> {

    public static final String ASSET = "asset";

    @Override
    public Result<Map<String, Object>> mapContext(PolicyContext context) {
        if (!(context instanceof AssetPolicyContext assetContext) || assetContext.asset() == null) {
            return Result.success(Map.of());
        }
        var asset = assetContext.asset();
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "id", asset.getId());
        putIfNotNull(map, "participantContextId", asset.getParticipantContextId());
        map.put("properties", asset.getProperties() == null ? Map.of() : asset.getProperties());
        map.put("privateProperties", asset.getPrivateProperties() == null ? Map.of() : asset.getPrivateProperties());
        return Result.success(Map.of(ASSET, map));
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
