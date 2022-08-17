/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on the method level, that designates that method to be a factory for a particular type.
 * The type is determined by the methods return type.
 * <p>
 * <p>
 * Methods annotated with {@code @Provider} must :
 * <ul>
 *     <li>be declared on an implementor of a {@linkplain ServiceExtension}</li>
 *     <li>have a non-void return type</li>
 *     <li>be public</li>
 *     <li>either have no parameters or accept a single {@linkplain ServiceExtensionContext}</li>
 * </ul>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {
    boolean isDefault() default false;
}
