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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DescriptionHandlerSettingsFactoryResult {
    private final DescriptionHandlerSettings descriptionHandlerSettings;
    private final List<String> errors;

    private DescriptionHandlerSettingsFactoryResult(
            @Nullable DescriptionHandlerSettings descriptionHandlerSettings,
            @Nullable List<String> errors) {
        this.descriptionHandlerSettings = descriptionHandlerSettings;
        this.errors = errors;
    }

    @Nullable
    public DescriptionHandlerSettings getDescriptionHandlerSettings() {
        return descriptionHandlerSettings;
    }

    @NotNull
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors != null ? errors : Collections.emptyList());
    }

    public static final class Builder {
        private DescriptionHandlerSettings descriptionHandlerSettings;
        private List<String> errors;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder descriptionHandlerSettings(@Nullable DescriptionHandlerSettings descriptionHandlerSettings) {
            this.descriptionHandlerSettings = descriptionHandlerSettings;
            return this;
        }

        public Builder errors(@Nullable List<String> errors) {
            this.errors = errors;
            return this;
        }

        public DescriptionHandlerSettingsFactoryResult build() {
            return new DescriptionHandlerSettingsFactoryResult(descriptionHandlerSettings, errors);
        }
    }
}
