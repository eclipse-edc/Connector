/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

import java.util.concurrent.TimeUnit;

interface DataPlaneTransferSyncConfiguration {

    String API_CONTEXT_ALIAS = "validation";

    @EdcSetting
    String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.dataplane.token.signer.privatekey.alias";

    @EdcSetting
    String DATA_PLANE_PUBLIC_API_ENDPOINT = "edc.transfer.dataplane.sync.endpoint";

    @EdcSetting
    String DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS = "edc.transfer.dataplane.sync.token.validity";
    long DEFAULT_DATA_PLANE_PUBLIC_API_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @EdcSetting
    String PUBLIC_KEY_ALIAS = "edc.public.key.alias";
}
