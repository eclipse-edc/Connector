/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.tooling.sample.module;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Extension;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

import static org.eclipse.dataspaceconnector.tooling.sample.module.SampleExtension.CATEGORY;
import static org.eclipse.dataspaceconnector.tooling.sample.module.SampleExtension.NAME;

/**
 * This is sample documentation.
 */
@Extension(value = NAME, categories = {CATEGORY})
@Provides({ProvidedService1.class})
public class SampleExtension implements ServiceExtension {
    public static final String NAME = "Sample Extension";
    public static final String CATEGORY = "sample";

    public static final String PREFIX = "edc.";

    @EdcSetting(value = "This is a sample configuration value", required = true)
    public static final String CONFIG1 = PREFIX + "config1";

    @Inject
    protected RequiredService requiredService;

    @Inject(required = false)
    protected OptionalService optionalService;

}
