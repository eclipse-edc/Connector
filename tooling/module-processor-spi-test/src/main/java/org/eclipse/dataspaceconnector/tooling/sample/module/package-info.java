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
 *       Microsoft Corporation - initial API and implementation
 *
 */

/**
 * This is a sample SPI package.
 */
@Spi(value = NAME, categories = {"category"})
package org.eclipse.dataspaceconnector.tooling.sample.module;

import org.eclipse.dataspaceconnector.spi.system.Spi;

import static org.eclipse.dataspaceconnector.tooling.sample.module.TestConstants.NAME;
