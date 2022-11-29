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

package org.eclipse.edc.connector.dataplane.transfer;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

import java.util.concurrent.TimeUnit;

public interface DataPlaneTransferConfig {

    @Setting(value = "Validity (in seconds) of tokens issued by the Control Plane for targetting the Data Plane public API")
    String TOKEN_VALIDITY_SECONDS = "edc.transfer.proxy.token.validity.seconds";
    long DEFAULT_TOKEN_VALIDITY_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @Setting(value = "Alias of private key used for signing tokens, retrieved from private key resolver")
    String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.proxy.token.signer.privatekey.alias";

    @Setting(value = "Alias of public key used for verifying the tokens, retrieved from the vault")
    String TOKEN_VERIFIER_PUBLIC_KEY_ALIAS = "edc.transfer.proxy.token.verifier.publickey.alias";

    @Setting(value = "Strategy for Data Plane instance selection")
    String DPF_SELECTOR_STRATEGY = "edc.transfer.client.selector.strategy";
    String DEFAULT_DPF_SELECTOR_STRATEGY = "random";
}