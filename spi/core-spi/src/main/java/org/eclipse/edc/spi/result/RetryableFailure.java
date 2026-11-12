/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.spi.result;

import java.util.List;

/**
 * A failure of an operation that could not be completed, e.g. because a remote service was not reachable, as opposed
 * to an operation that was completed with a negative outcome. The same operation may succeed later.
 */
public class RetryableFailure extends Failure {

    public RetryableFailure(List<String> messages) {
        super(messages);
    }
}
