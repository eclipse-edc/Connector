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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;

public class ExtensionWithConfigValue implements ServiceExtension {

    public static final String DEFAULT_VALUE = "default-value";
    @Setting(key = "test.key")
    private String requiredVal;

    @Setting(key = "test.optional.value", required = false)
    private Long optionalVal;

    @Setting(key = "test.key2", defaultValue = DEFAULT_VALUE)
    private String requiredValWithDefault;

    @Setting(key = "test.key3", defaultValue = DEFAULT_VALUE)
    private Double requiredDoubleVal;
}
