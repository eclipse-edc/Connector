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

package org.eclipse.edc.tck.dsp.guard;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An action that is triggered when the predicate matches a condition.
 */
public record Trigger<T>(Predicate<Object> predicate, Consumer<T> action) {
}
