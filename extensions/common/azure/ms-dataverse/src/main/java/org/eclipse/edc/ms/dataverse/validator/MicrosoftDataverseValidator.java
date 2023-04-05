/*
 *  Copyright (c) 2023 Microsoft Corporation
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

package org.eclipse.edc.ms.dataverse.validator;

import org.eclipse.edc.util.string.StringUtils;

/**
 * Validates dataverse entity names, service uri, service principal id.
 * <p>
 * See <a href="https://learn.microsoft.com/en-us/power-apps/developer/data-platform/reference/entities/entity">
 * Azure documentation</a>.
 */
public class MicrosoftDataverseValidator {
    private static final int ENTITY_NAME_MIN_LENGTH = 1;
    private static final int ENTITY_NAME_MAX_LENGTH = 128;
    private static final int SERVICE_URI_MIN_LENGTH = 1;
    private static final int SERVICE_URI_MAX_LENGTH = 1024;
    private static final int SERVICE_PRINCIPAL_ID_MIN_LENGTH = 36;
    private static final int SERVICE_PRINCIPAL_ID_MAX_LENGTH = 36;
    public static final String ENTITY_NAME = "entity";
    public static final String SERVICE_URI = "serviceUri";
    public static final String SERVICE_PRINCIPAL_ID = "servicePrincipalId";
    private static final String INVALID_RESOURCE_NAME_LENGTH = "Invalid %s name length, the name must be between %s and %s characters long";
    private static final String RESOURCE_NAME_EMPTY = "Invalid %s name, the name may not be null, empty or blank";

    /**
     * Checks if an entity name is valid.
     *
     * @param entityName A String representing the entity name to validate.
     */
    public static void validateEntityName(String entityName) {
        checkLength(entityName, ENTITY_NAME, ENTITY_NAME_MIN_LENGTH, ENTITY_NAME_MAX_LENGTH);
    }

    /**
     * Checks if a service uri is valid.
     *
     * @param serviceUri A String representing the service uri to validate.
     */
    public static void validateServiceUri(String serviceUri) {
        checkLength(serviceUri, SERVICE_URI, SERVICE_URI_MIN_LENGTH, SERVICE_URI_MAX_LENGTH);
    }

    /**
     * Checks if a service principal id is valid.
     *
     * @param servicePrincipalId A String representing the service to validate.
     */
    public static void validateServicePrincipalId(String servicePrincipalId) {
        checkLength(servicePrincipalId, SERVICE_PRINCIPAL_ID, SERVICE_PRINCIPAL_ID_MIN_LENGTH, SERVICE_PRINCIPAL_ID_MAX_LENGTH);
    }

    private static void checkLength(String name, String resourceType, int minLength, int maxLength) {
        if (StringUtils.isNullOrBlank(name)) {
            throw new IllegalArgumentException(String.format(RESOURCE_NAME_EMPTY, resourceType));
        }

        if (name.length() < minLength || name.length() > maxLength) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME_LENGTH, resourceType, minLength, maxLength));
        }
    }
}
