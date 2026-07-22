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

package org.eclipse.edc.policy.cel.function;

/**
 * Value types that can be used in the signature of a {@link CelFunction}.
 * <p>
 * This is an EDC-neutral abstraction over the underlying CEL type system, so that modules contributing custom
 * functions do not need a compile dependency on the CEL implementation.
 */
public enum CelValueType {
    STRING,
    BOOL,
    INT,
    DOUBLE,
    LIST,
    MAP,
    DYN
}
