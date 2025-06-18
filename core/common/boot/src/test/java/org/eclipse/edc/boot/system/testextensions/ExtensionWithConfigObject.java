/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.testextensions;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.ServiceExtension;

public class ExtensionWithConfigObject implements ServiceExtension {

    @Configuration
    private ConfigurationObject configurationObject;

    @Configuration // is optional because all @Settings within are optionsl
    private OptionalConfigurationObject optionalConfigurationObject;

    @SettingContext("custom.context")
    @Configuration
    private ConfigurationObject configurationObjectWithContext;

    public ConfigurationObject getConfigurationObject() {
        return configurationObject;
    }

    @Settings
    public static class OptionalConfigurationObject {
        @Setting(key = "optional.key", required = false)
        private String val;

        public String getVal() {
            return val;
        }
    }
}
