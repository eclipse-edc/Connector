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

package org.eclipse.dataspaceconnector.azure.blob.core.validator;


import org.eclipse.dataspaceconnector.common.string.StringUtils;

import java.util.Base64;
import java.util.regex.Pattern;


/**
 * Validates storage account resource names and keys.
 * <p>
 * See <a href="https://docs.microsoft.com/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata">
 * Azure documentation</a>.
 */
public class AzureStorageValidator {
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private static final int ACCOUNT_MIN_LENGTH = 3;
    private static final int ACCOUNT_MAX_LENGTH = 24;
    private static final int CONTAINER_MIN_LENGTH = 3;
    private static final int CONTAINER_MAX_LENGTH = 63;
    private static final int BLOB_MIN_LENGTH = 1;
    private static final int BLOB_MAX_LENGTH = 1024;
    private static final Pattern ACCOUNT_REGEX = Pattern.compile("^[a-z0-9]+$");
    private static final Pattern CONTAINER_REGEX = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private static final String ACCOUNT = "account";
    private static final String BLOB = "blob";
    private static final String CONTAINER = "container";
    private static final String INVALID_KEY = "Storage Key is not a valid base64 encoded string";
    private static final String INVALID_RESOURCE_NAME = "Invalid %s name";
    private static final String INVALID_RESOURCE_NAME_LENGTH = "Invalid %s name length, the name must be between %s and %s characters long";
    private static final String RESOURCE_NAME_EMPTY = "Invalid %s name, the name may not be null, empty or blank";
    private static final String TOO_MANY_PATH_SEGMENTS = "The number of URL path segments (strings between '/' characters) as part of the blob name cannot exceed 254.";

    /**
     * Checks if an account name is valid.
     *
     * @param accountName A String representing the account name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid account name.
     */
    public static void validateAccountName(String accountName) {
        checkLength(accountName, ACCOUNT, ACCOUNT_MIN_LENGTH, ACCOUNT_MAX_LENGTH);

        if (!ACCOUNT_REGEX.matcher(accountName).matches()) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, ACCOUNT));
        }
    }

    /**
     * Checks if a container name is valid.
     *
     * @param containerName A String representing the container name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid container name.
     */
    public static void validateContainerName(String containerName) {
        if (!("$root".equals(containerName) || "$logs".equals(containerName) || "$web".equals(containerName))) {
            checkLength(containerName, CONTAINER, CONTAINER_MIN_LENGTH, CONTAINER_MAX_LENGTH);

            if (!CONTAINER_REGEX.matcher(containerName).matches()) {
                throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, CONTAINER));
            }
        }
    }

    /**
     * Checks if a blob name is valid.
     *
     * @param blobName A String representing the blob name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid blob name.
     */
    public static void validateBlobName(String blobName) {
        checkLength(blobName, BLOB, BLOB_MIN_LENGTH, BLOB_MAX_LENGTH);

        var slashCount = blobName.chars().filter(ch -> ch == '/').count();

        if (slashCount >= 254) {
            throw new IllegalArgumentException(TOO_MANY_PATH_SEGMENTS);
        }
    }

    /**
     * Checks if a storage account shared key is in the expected format.
     *
     * @param accountKey A String representing the shared key to validate.
     * @throws IllegalArgumentException if the string does not represent a value encoded in the expected format.
     */
    public static void validateSharedKey(String accountKey) {
        if (StringUtils.isNullOrBlank(accountKey)) {
            throw new IllegalArgumentException(INVALID_KEY);
        }
        try {
            DECODER.decode(accountKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(INVALID_KEY);
        }
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
