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

package org.eclipse.dataspaceconnector.ids.core.configuration;

import java.util.Objects;

/**
 * The {@link IllegalSettingException} indicates that a configuration parameter was set to an invalid value.
 * </p>
 * The {@link IllegalSettingException} does not extend the {@link org.eclipse.dataspaceconnector.spi.EdcException} interface,
 * because it's not a {@link RuntimeException} and should never be unchecked.
 */
public class IllegalSettingException extends Exception {
    private final String settingKey;

    public IllegalSettingException(String settingKey, String message) {
        super(message);
        this.settingKey = Objects.requireNonNull(settingKey, "settingKey is required");
    }

    public IllegalSettingException(String settingKey, String message, Throwable cause) {
        super(message, cause);
        this.settingKey = Objects.requireNonNull(settingKey, "settingKey is required");
    }

    public String getSettingKey() {
        return settingKey;
    }
}
