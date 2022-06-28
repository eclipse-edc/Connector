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

public interface DataPlaneTransferSyncConfig {

    @EdcSetting
    String DATA_PROXY_TOKEN_VALIDITY_SECONDS = "edc.transfer.proxy.token.validity.seconds";
    long DEFAULT_DATA_PROXY_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @EdcSetting
    String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.proxy.token.signer.privatekey.alias";

    @EdcSetting
    String TOKEN_VERIFIER_PUBLIC_KEY_ALIAS = "edc.transfer.proxy.token.verifier.publickey.alias";
}
