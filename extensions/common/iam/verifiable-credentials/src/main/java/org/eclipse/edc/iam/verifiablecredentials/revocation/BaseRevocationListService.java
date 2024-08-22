/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.revocation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.Cache;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.success;

/**
 * Service to check if a particular {@link VerifiableCredential} is "valid", where "validity" is defined as not revoked and not suspended nor having
 * any other status. Credentials that don't have a {@code credentialStatus} object are deemed "valid" as well.
 * <p>
 * To achieve that, the {@link VerifiableCredential#getCredentialStatus()} object is inspected and checked against the status list credential referenced therein.
 * <p>
 * To limit traffic on the actual StatusList credential, it is cached in a thread-safe {@link Map}, and only re-downloaded if the cache is expired.
 * <p>
 * Currently, StatusList2021 and BitStringStatusList are supported.
 */
public abstract class BaseRevocationListService<C extends VerifiableCredential, S> implements RevocationListService {
    private final Cache<String, C> statusListCredentialCache;
    private final Class<C> credentialClass;
    private final ObjectMapper objectMapper;

    protected BaseRevocationListService(ObjectMapper mapper, long cacheValidity, Class<C> credentialClass) {
        this.objectMapper = mapper.copy()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // technically, credential subjects and credential status can be objects AND Arrays
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // let's make sure this is disabled, because the "@context" would cause problems
        statusListCredentialCache = new Cache<>(this::downloadStatusListCredential, cacheValidity);
        this.credentialClass = credentialClass;
    }

    @Override
    public Result<Void> checkValidity(CredentialStatus credential) {
        var credentialStatus = getCredentialStatus(credential);
        var credentialIndex = getStatusIndex(credentialStatus);
        return preliminaryChecks(credentialStatus)
                .compose(v -> validateStatusPurpose(credentialStatus))
                .compose(v -> getStatusEntryValue(credentialStatus))
                .compose(status -> status != null ?
                        Result.failure("Credential status is '%s', status at index %d is '1'".formatted(status, credentialIndex)) :
                        Result.success());
    }

    @Override
    public Result<String> getStatusPurpose(VerifiableCredential credential) {
        if (credential.getCredentialStatus().isEmpty()) {
            return success(null);
        }

        var res = credential.getCredentialStatus().stream()
                .map(this::getCredentialStatus)
                .map(this::getStatusEntryValue)
                .collect(Collectors.groupingBy(AbstractResult::succeeded));

        if (res.containsKey(false)) { //if any failed
            return Result.failure(res.get(false).stream().map(AbstractResult::getFailureDetail).toList());
        }

        var list = res.get(true).stream()
                .filter(r -> r.getContent() != null)
                .map(AbstractResult::getContent).toList();

        return list.isEmpty() ? success(null) : success(String.join(", ", list));
    }

    protected abstract S getCredentialStatus(CredentialStatus credentialStatus);

    protected Result<Void> preliminaryChecks(S credentialStatus) {
        return Result.success();
    }

    /**
     * Gets a statuslist credential from the cache, of if it's not there yet, downloads it.
     *
     * @param credentialUrl the URL from where to download the cred
     * @return the VerifiableCredential
     * @throws EdcException if it could not be downloaded
     */
    protected C getCredential(String credentialUrl) {
        var credential = statusListCredentialCache.get(credentialUrl);
        // credential is cached, but expired -> download again
        if (credential != null && credential.getExpirationDate() != null && credential.getExpirationDate().isBefore(Instant.now())) {
            statusListCredentialCache.evict(credentialUrl);
        }
        return statusListCredentialCache.get(credentialUrl);

    }

    /**
     * Obtains the status purpose for a particular credentialStatus entry if it is set, otherwise returns a successful result with a {@code null} content.
     * So, a successful result with a non-null content indicates, that the respective credentialStatus is set.
     *
     * @param credentialStatus the credentialStatus object of the VC (not the StatusList credential!)
     */
    protected abstract Result<String> getStatusEntryValue(S credentialStatus);

    /**
     * Validates, that the statusPurpose of the credentialStatus is equal to the one found in the StatusList Credential
     *
     * @param credentialStatus the credentialStatus object of the VC (not the StatusList credential!)
     */
    protected abstract Result<Void> validateStatusPurpose(S credentialStatus);

    /**
     * Gets the {@code statusIndex} entry of the VC's credentialStatus object.
     *
     * @param credentialStatus the credentialStatus object of the VC (not the StatusList credential!)
     * @return the statusIndex entry
     */
    protected abstract int getStatusIndex(S credentialStatus);

    private C downloadStatusListCredential(String credentialUrl) {
        try {
            return objectMapper.readValue(URI.create(credentialUrl).toURL(), credentialClass);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
