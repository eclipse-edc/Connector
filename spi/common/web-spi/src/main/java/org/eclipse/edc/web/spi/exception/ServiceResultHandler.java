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

package org.eclipse.edc.web.spi.exception;

import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ServiceResultHandler {

    /**
     * Utility method for calling {@link #mapToException(ServiceFailure, Class, String)} when id is null.
     *
     * @param failure The {@link ServiceFailure}
     * @param clazz The type in whose context the failure occurred. Must not be null.
     * @return Exception mapped from failure reason.
     */
    public static EdcException mapToException(@NotNull ServiceFailure failure, @NotNull Class<?> clazz) {
        return mapToException(failure, clazz, null);
    }

    /**
     * Interprets a {@link ServiceResult#reason()} property and returns the
     * appropriate exception:
     * <table>
     *   <tr>
     *     <th>reason</th> <th>exception</th>
     *   </tr>
     *   <tr>
     *     <td>NOT_FOUND </td> <td>ObjectNotFoundException</td>
     *   </tr>
     *   <tr>
     *     <td>CONFLICT</td> <td>ObjectExistsException</td>
     *   </tr>
     *   <tr>
     *     <td>BAD_REQUEST</td> <td>InvalidRequestException</td>
     *   </tr>
     *   <tr>
     *     <td>other</td> <td>EdcException</td>
     *   </tr>
     *   <caption>Mapping from failure reason to exception</caption>
     * </table>
     *
     * @param failure The {@link ServiceFailure}
     * @param clazz The type in whose context the failure occurred. Must not be null.
     * @param id The id of the entity which was involved in the failure. Can be null for
     *         {@link ServiceFailure.Reason#BAD_REQUEST}.
     * @return Exception mapped from failure reason.
     */
    public static EdcException mapToException(@NotNull ServiceFailure failure, @NotNull Class<?> clazz, @Nullable String id) {
        switch (failure.getReason()) {
            case NOT_FOUND:
                return new ObjectNotFoundException(clazz, id);
            case CONFLICT:
                return new ObjectConflictException(failure.getMessages());
            case BAD_REQUEST:
                return new InvalidRequestException(failure.getMessages());
            default:
                return new EdcException("unexpected error: " + failure.getFailureDetail());
        }
    }

    /**
     * Convenience method to avoid specify the id when it does not exist.
     *
     * @param clazz The type in whose context the failure occurred. Must not be null.
     * @return The mapper {@link Function}
     */

    public static Function<ServiceFailure, EdcException> exceptionMapper(@NotNull Class<?> clazz) {
        return (serviceFailure -> mapToException(serviceFailure, clazz, null));
    }

    /**
     * Returns a function that can be use as mapper for handling exception in context like {@link  ServiceResult#orElseThrow(Function)}
     *
     * @param clazz The type in whose context the failure occurred. Must not be null.
     * @param id The id of the entity which was involved in the failure. Can be null for
     *           {@link ServiceFailure.Reason#BAD_REQUEST}.
     * @return The mapper {@link Function}
     */

    public static Function<ServiceFailure, EdcException> exceptionMapper(@NotNull Class<?> clazz, @Nullable String id) {
        return (serviceFailure -> mapToException(serviceFailure, clazz, id));
    }
}
