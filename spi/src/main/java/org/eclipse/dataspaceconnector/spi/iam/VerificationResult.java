/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a token verification.
 */
public class VerificationResult {
    private final boolean valid;
    private ClaimToken token;
    private List<String> error = new ArrayList<>();

    public VerificationResult(ClaimToken token) {
        valid = true;
        this.token = token;
    }

    public VerificationResult(String... errors) {
        this(List.of(errors));
    }

    public VerificationResult(List<String> errors) {
        valid = false;
        error = errors;
    }


    public ClaimToken token() {
        return token;
    }

    public boolean valid() {
        return valid;
    }

    public List<String> errors() {
        return Collections.unmodifiableList(error);
    }
}


