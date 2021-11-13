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
package org.eclipse.dataspaceconnector.spi.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a policy evaluation operation.
 */
public class PolicyResult {
    private final List<String> problems;

    public PolicyResult() {
        problems = Collections.emptyList();
    }

    public PolicyResult(List<String> problems) {
        this.problems = new ArrayList<>(problems);
    }

    public boolean valid() {
        return problems.isEmpty();
    }
}
