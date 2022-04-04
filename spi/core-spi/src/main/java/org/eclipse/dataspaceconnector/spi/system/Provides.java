/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies which feature a certain class, package or modules provides.
 * Feature must be namespaced in the form "edc:XXX:YYY:ZZZ".
 * <p>
 * If a referenced feature class is <em>not</em> annotated, the dependency injection mechanism will use the feature's fully
 * qualified class name by default.
 * <p>
 * All fields that are marked with {@link Inject} must be @Provided by at least one extension.
 */
@Target({ ElementType.TYPE, ElementType.PACKAGE, ElementType.MODULE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Provides {
    Class<?>[] value();
}
