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

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;

final class SettingDefaults {
    public static final String EDC_IDS_ID = "urn:connector:edc";
    public static final String EDC_IDS_TITLE = "Eclipse Dataspace Connector";
    public static final String EDC_IDS_DESCRIPTION = "Eclipse Dataspace Connector";
    public static final String EDC_IDS_MAINTAINER = "https://example.com";
    public static final String EDC_IDS_CURATOR = IdsId.participant("curator").getValue();
    public static final String EDC_IDS_ENDPOINT = "https://example.com";
    public static final String SECURITY_PROFILE = SecurityProfile.BASE_SECURITY_PROFILE.getValue();
    public static final String EDC_IDS_CATALOG_ID = "default";
}
