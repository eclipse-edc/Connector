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
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a way to retrieve the default service implementation, to be used when an extended implementation cannot be
 * found.
 */
@FunctionalInterface
public interface DefaultServiceSupplier {

    /**
     * Provides a default service for the passed type
     *
     * @param type the class object of the needed type
     * @return a default service, null if not found
     */
    @Nullable
    Object provideFor(InjectionPoint<?> type, ServiceExtensionContext context);
}
