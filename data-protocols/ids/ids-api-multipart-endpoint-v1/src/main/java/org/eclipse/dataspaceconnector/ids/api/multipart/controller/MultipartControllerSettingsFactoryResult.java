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

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class MultipartControllerSettingsFactoryResult {
    private final MultipartControllerSettings settings;
    private final List<String> errors;

    private MultipartControllerSettingsFactoryResult(
            @Nullable MultipartControllerSettings settings,
            @Nullable List<String> errors) {
        this.settings = settings;
        this.errors = errors;
    }

    @Nullable
    public MultipartControllerSettings getSettings() {
        return settings;
    }

    @NotNull
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors != null ? errors : Collections.emptyList());
    }

    static final class Builder {
        private MultipartControllerSettings settings;
        private List<String> errors;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder settings(@Nullable MultipartControllerSettings settings) {
            this.settings = settings;
            return this;
        }

        public Builder errors(@Nullable List<String> errors) {
            this.errors = errors;
            return this;
        }

        public MultipartControllerSettingsFactoryResult build() {
            return new MultipartControllerSettingsFactoryResult(settings, errors);
        }
    }
}
