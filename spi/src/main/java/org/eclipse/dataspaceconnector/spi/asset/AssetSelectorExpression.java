/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.asset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Labels are used to trim down the selection of assets. If the carried hash
 * of labels is empty all available assets are eligible for selection.
 */
public final class AssetSelectorExpression {
    private final Map<String, String> filterLabels = new HashMap<>();

    private AssetSelectorExpression(final Map<String, String> filterLabels) {
        Optional.ofNullable(filterLabels).ifPresent(this.filterLabels::putAll);
    }

    public Map<String, String> getFilterLabels() {
        return Collections.unmodifiableMap(filterLabels);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, String> labels = new HashMap<>();

        private Builder() {
        }

        public Builder filterByLabel(final String key, final String value) {
            this.labels.put(key, value);
            return this;
        }

        public AssetSelectorExpression build() {
            return new AssetSelectorExpression(labels);
        }
    }
}
