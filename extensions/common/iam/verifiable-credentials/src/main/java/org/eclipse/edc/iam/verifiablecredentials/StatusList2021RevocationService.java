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

package org.eclipse.edc.iam.verifiablecredentials;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist.StatusListCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist.StatusListStatus;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service to check if a particular {@link VerifiableCredential} is "valid", where "validity" is defined as not revoked and not suspended.
 * Credentials, that don't have a {@code credentialStatus} object are deemed "valid" as well.
 * <p>
 * To achieve that, the {@link VerifiableCredential#getCredentialStatus()} object is inspected and checked against the status list credential referenced therein.
 * <p>
 * To limit traffic on the actual StatusList2021 credential, it is cached in a thread-safe {@link Map}, and only re-downloaded if the cache is expired.
 */
public class StatusList2021RevocationService implements RevocationListService {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, TimestampedValue> cache = new HashMap<>();

    private final ObjectMapper objectMapper;
    private final long cacheValidityMillis;

    public StatusList2021RevocationService(ObjectMapper objectMapper, long cacheValidityMillis) {
        this.objectMapper = objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // let's make sure this is disabled, because the "@context" would cause problems
        this.cacheValidityMillis = cacheValidityMillis;
    }

    @Override
    public Result<Void> checkValidity(VerifiableCredential credential) {
        return credential.getCredentialStatus().stream().map(StatusListStatus::parse)
                .map(this::checkStatus)
                .reduce(Result::merge)
                .orElse(Result.failure("Could not check the validity of the credential with ID '%s'".formatted(credential.getId())));
    }

    private Result<Void> checkStatus(StatusListStatus status) {
        var statusListCredUrl = status.getStatusListCredential();
        var credential = cacheGet(statusListCredUrl);
        var statuslistCredential = StatusListCredential.parse(credential);

        // check that the "statusPurpose" values match
        var credentialPurpose = status.getStatusListPurpose();
        var statusListCredentialPurpose = statuslistCredential.statusPurpose();
        if (!credentialPurpose.equalsIgnoreCase(statusListCredentialPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the status list's purpose: '%s' != '%s'".formatted(credentialPurpose, statusListCredentialPurpose));
        }

        // check that the value at index in the bitset is "1"
        var bytes = Base64.getUrlDecoder().decode(statuslistCredential.encodedList());
        var bitset = BitSet.valueOf(bytes);
        var index = status.getStatusListIndex();
        if (bitset.get(index)) {
            return Result.failure("Credential status is '%s', status at index %d is false".formatted(credentialPurpose, index));
        }
        return Result.success();
    }

    private VerifiableCredential downloadCredential(String url) {
        try {
            return objectMapper.readValue(URI.create(url).toURL(), VerifiableCredential.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VerifiableCredential cacheGet(String statusListCredentialUrl) {
        VerifiableCredential value;
        lock.readLock().lock();
        try {
            if (isEntryExpired(statusListCredentialUrl)) {
                lock.readLock().unlock(); // unlock read, acquire write -> "upgrade" lock
                lock.writeLock().lock();
                try {
                    if (isEntryExpired(statusListCredentialUrl)) {
                        var newCred = downloadCredential(statusListCredentialUrl);
                        cache.put(statusListCredentialUrl, new TimestampedValue(newCred));
                    }
                } finally {
                    lock.readLock().lock(); // downgrade lock
                    lock.writeLock().unlock();
                }
            }

            value = cache.get(statusListCredentialUrl).verifiableCredential();
        } finally {
            lock.readLock().unlock();
        }
        return value;
    }

    private boolean isEntryExpired(String key) {
        var timestampedValue = cache.get(key);
        if (timestampedValue == null) return true;
        var lastCacheUpdate = timestampedValue.lastUpdatedAt;
        return lastCacheUpdate == null || lastCacheUpdate.plus(cacheValidityMillis, ChronoUnit.MILLIS).isBefore(Instant.now());
    }

    private record TimestampedValue(VerifiableCredential verifiableCredential, Instant lastUpdatedAt) {
        TimestampedValue(VerifiableCredential credential) {
            this(credential, Instant.now());
        }
    }
}
