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

    // TODO TP:01-01 is failing because we don't send endpoint in data address
    List<String> ALLOWED_FAILURES = List.of(
            "TP:01-01", "TP:01-02", "TP:01-03", "TP:01-04", "TP:01-05",
            "TP:02-01", "TP:02-02", "TP:02-03", "TP:02-04",
            "TP:03-03", "TP:03-04", "TP:03-05", "TP:03-06",
            "TP_C:01-04",
            "TP_C:02-04");

}
