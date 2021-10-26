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

package org.eclipse.dataspaceconnector.ids.api.multipart.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BaseConnectorFactorySettingsFactoryResult {
    private final BaseConnectorFactorySettings baseConnectorFactorySettings;
    private final List<String> errors;

    private BaseConnectorFactorySettingsFactoryResult(
            @Nullable BaseConnectorFactorySettings baseConnectorFactorySettings,
            @Nullable List<String> errors) {
        this.baseConnectorFactorySettings = baseConnectorFactorySettings;
        this.errors = errors;
    }

    @Nullable
    public BaseConnectorFactorySettings getBaseConnectorFactorySettings() {
        return baseConnectorFactorySettings;
    }

    @NotNull
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors != null ? errors : Collections.emptyList());
    }

    public static final class Builder {
        private BaseConnectorFactorySettings baseConnectorFactorySettings;
        private List<String> errors;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder baseConnectorFactorySettings(@Nullable BaseConnectorFactorySettings baseConnectorFactorySettings) {
            this.baseConnectorFactorySettings = baseConnectorFactorySettings;
            return this;
        }

        public Builder errors(@Nullable List<String> errors) {
            this.errors = errors;
            return this;
        }

        public BaseConnectorFactorySettingsFactoryResult build() {
            return new BaseConnectorFactorySettingsFactoryResult(baseConnectorFactorySettings, errors);
        }
    }
}
