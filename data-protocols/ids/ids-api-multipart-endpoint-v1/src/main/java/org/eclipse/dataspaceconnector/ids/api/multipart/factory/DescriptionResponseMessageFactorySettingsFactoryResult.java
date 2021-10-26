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

public class DescriptionResponseMessageFactorySettingsFactoryResult {
    private final DescriptionResponseMessageFactorySettings descriptionResponseMessageFactorySettings;
    private final List<String> errors;

    private DescriptionResponseMessageFactorySettingsFactoryResult(
            @Nullable DescriptionResponseMessageFactorySettings descriptionResponseMessageFactorySettings,
            @Nullable List<String> errors) {
        this.descriptionResponseMessageFactorySettings = descriptionResponseMessageFactorySettings;
        this.errors = errors;
    }

    @Nullable
    public DescriptionResponseMessageFactorySettings getDescriptionResponseMessageFactorySettings() {
        return descriptionResponseMessageFactorySettings;
    }

    @NotNull
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors != null ? errors : Collections.emptyList());
    }

    public static final class Builder {
        private DescriptionResponseMessageFactorySettings descriptionResponseMessageFactorySettings;
        private List<String> errors;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder descriptionResponseMessageFactorySettings(@Nullable DescriptionResponseMessageFactorySettings descriptionResponseMessageFactorySettings) {
            this.descriptionResponseMessageFactorySettings = descriptionResponseMessageFactorySettings;
            return this;
        }

        public Builder errors(@Nullable List<String> errors) {
            this.errors = errors;
            return this;
        }

        public DescriptionResponseMessageFactorySettingsFactoryResult build() {
            return new DescriptionResponseMessageFactorySettingsFactoryResult(descriptionResponseMessageFactorySettings, errors);
        }
    }
}
