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

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public final class SettingKeys {
    @EdcSetting
    public static final String EDC_IDS_ID = "edc.ids.id";

    @EdcSetting
    public static final String EDC_IDS_TITLE = "edc.ids.title";

    @EdcSetting
    public static final String EDC_IDS_DESCRIPTION = "edc.ids.description";

    @EdcSetting
    public static final String EDC_IDS_MAINTAINER = "edc.ids.maintainer";

    @EdcSetting
    public static final String EDC_IDS_CURATOR = "edc.ids.curator";

    @EdcSetting
    public static final String EDC_IDS_ENDPOINT = "edc.ids.endpoint";

    @EdcSetting
    public static final String EDC_IDS_SECURITY_PROFILE = "edc.ids.security.profile";
}
