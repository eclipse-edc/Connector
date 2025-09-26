/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.spi.system.ServiceExtension;
import org.jetbrains.annotations.Nullable;

public record InjectionFailure(ServiceExtension dependent, InjectionPoint<ServiceExtension> dependency, @Nullable String failureDetail) {
}
