/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Microsoft Corporation - moved to current location
 *
 */

package org.eclipse.edc.junit.annotations;

import org.junit.jupiter.api.Tag;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Postgresql integration testing. It applies specific Junit Tag.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Testcontainers
@Tag("PostgresqlIntegrationTest")
public @interface PostgresqlDbIntegrationTest {
}
