/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core.service;

import org.eclipse.edc.spi.result.Result;

class JsonLdPresentationVerifier {

    JsonLdPresentationVerifier() {
    }

    public Result<Void> verifyPresentation(String rawVp) {
        throw new UnsupportedOperationException("not yet implemented!");
    }
}
