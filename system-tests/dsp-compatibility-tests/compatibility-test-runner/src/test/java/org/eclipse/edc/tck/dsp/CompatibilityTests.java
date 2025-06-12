/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp;

import java.util.List;

public interface CompatibilityTests {

    // TODO Those remaining failures are due the fact that EDC consider the DEPROVISIONED state completed while the TCK expects terminated.
    List<String> ALLOWED_FAILURES = List.of(
            "TP:01-01", "TP:01-03", "TP:01-05");

}
