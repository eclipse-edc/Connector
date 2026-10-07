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

package org.eclipse.edc.connector.controlplane.asset.spi.index;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compatibility layer for queries that still use the legacy {@link Asset#PROPERTY_ID} property as left operand
 * (or sort field), which is no longer stored in the {@link Asset} properties. Those are translated to the asset id.
 * A deprecation warning is logged only the first time a legacy usage is translated by this instance.
 */
@SuppressWarnings("deprecation")
public final class AssetIdCompatibility {

    public static final String ASSET_ID_FIELD = "id";
    private static final String DEPRECATION_WARNING = "Querying assets by '%s' is deprecated and it will be translated to '%s'. Please use '%s' instead."
            .formatted(Asset.PROPERTY_ID, ASSET_ID_FIELD, ASSET_ID_FIELD);

    private final Monitor monitor;
    private final AtomicBoolean warned = new AtomicBoolean(false);

    public AssetIdCompatibility(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Whether the operand refers to the legacy {@link Asset#PROPERTY_ID} property, either plain or wrapped in single quotes
     * (the closing quote is optional, as it is for property lookups).
     */
    public static boolean isLegacyIdOperand(Object operand) {
        if (!(operand instanceof String key)) {
            return false;
        }
        if (key.startsWith("'")) {
            key = key.endsWith("'") && key.length() > 1 ? key.substring(1, key.length() - 1) : key.substring(1);
        }
        return Asset.PROPERTY_ID.equals(key);
    }

    /**
     * Translates criteria that use the legacy {@link Asset#PROPERTY_ID} as left operand to the asset id.
     */
    public List<Criterion> translate(List<Criterion> criteria) {
        if (criteria == null || criteria.stream().noneMatch(AssetIdCompatibility::isLegacy)) {
            return criteria;
        }
        warnOnce();
        return translateCriteria(criteria);
    }

    /**
     * Translates the filter and sort field of the {@link QuerySpec} that use the legacy {@link Asset#PROPERTY_ID} to the asset id.
     */
    public QuerySpec translate(QuerySpec querySpec) {
        var legacySortField = isLegacyIdOperand(querySpec.getSortField());
        if (!legacySortField && querySpec.getFilterExpression().stream().noneMatch(AssetIdCompatibility::isLegacy)) {
            return querySpec;
        }
        warnOnce();
        return QuerySpec.Builder.newInstance()
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .sortOrder(querySpec.getSortOrder())
                .sortField(legacySortField ? ASSET_ID_FIELD : querySpec.getSortField())
                .filter(translateCriteria(querySpec.getFilterExpression()))
                .build();
    }

    /**
     * Logs the deprecation warning about the usage of the legacy {@link Asset#PROPERTY_ID}, only the first time it's called.
     */
    public void warnOnce() {
        if (warned.compareAndSet(false, true)) {
            monitor.warning(DEPRECATION_WARNING);
        }
    }

    private static boolean isLegacy(Criterion criterion) {
        return isLegacyIdOperand(criterion.getOperandLeft());
    }

    private static List<Criterion> translateCriteria(List<Criterion> criteria) {
        return criteria.stream()
                .map(c -> isLegacy(c) ? c.withLeftOperand(ASSET_ID_FIELD) : c)
                .toList();
    }
}
