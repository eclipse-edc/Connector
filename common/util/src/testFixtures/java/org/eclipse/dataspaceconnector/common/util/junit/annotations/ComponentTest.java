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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.common.util.junit.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for ComponentTest. The tests with the ComponentTest tag do not use an external system but uses
 * collaborator objects instead of mocks. For example in-memory implementations. It applies a specific Junit Tag.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("ComponentTest")
public @interface ComponentTest {
}
