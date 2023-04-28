/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.did.web;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

/**
 * Defines configuration keys used by the Web DID extension.
 */
public interface ConfigurationKeys {

    /**
     * If set, the resolver will use the endpoint to resolve DIDs using DNS over HTTPS.
     */
    @Setting(value = "specification of endpoint to resolve DIDs using DNS over HTTPS")
    String DNS_OVER_HTTPS = "edc.webdid.doh.url";

}
