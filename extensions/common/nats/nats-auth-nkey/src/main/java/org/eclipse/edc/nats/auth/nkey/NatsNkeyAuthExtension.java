/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.nats.auth.nkey;

import io.nats.client.NKey;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.edc.nats.auth.nkey.NatsNkeyAuthExtension.NAME;

/**
 * Contributes NKey-based authentication to every NATS-connecting extension. The seed (the NKey private key) is read
 * from a file — typically projected into the container by the deployment (e.g. a Vault-agent init container) — and
 * used to answer the server's challenge: the client signs the server nonce with the seed, the server verifies the
 * signature against the user's public key in its {@code authorization} block. The seed itself never leaves the
 * process.
 * <p>
 * The resulting {@link Options} are registered as a service and picked up by the NATS event publisher and the task
 * publisher/subscriber extensions, which copy them via {@link Options.Builder#Builder(Options)} and set their own
 * server URL. When no seed is configured, no service is registered and connections are made without credentials.
 */
@Extension(value = NAME)
@Provides(Options.class)
public class NatsNkeyAuthExtension implements ServiceExtension {
    public static final String NAME = "NATS NKey Authentication Extension";

    static final String SEED_PATH_KEY = "edc.nats.auth.nkey.seed.path";

    @Setting(key = SEED_PATH_KEY, required = false, description = "Path to a file containing the NKey seed used to authenticate NATS connections. When not set, connections are made without credentials.")
    private String seedPath;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (seedPath == null) {
            monitor.info("'%s' is not configured, NATS connections will not present credentials".formatted(SEED_PATH_KEY));
            return;
        }

        char[] seed;
        try {
            seed = Files.readString(Path.of(seedPath)).trim().toCharArray();
        } catch (IOException e) {
            throw new EdcException("Error reading NKey seed file '%s'".formatted(seedPath), e);
        }
        if (seed.length == 0) {
            throw new EdcException("NKey seed file '%s' is empty".formatted(seedPath));
        }
        // fail fast at startup on a malformed seed instead of on the first connection attempt
        try {
            NKey.fromSeed(seed).getPublicKey();
        } catch (Exception e) {
            throw new EdcException("File '%s' does not contain a valid NKey seed".formatted(seedPath), e);
        }

        var options = new Options.Builder()
                .authHandler(Nats.staticCredentials(null, seed))
                .build();
        context.registerService(Options.class, options);
        monitor.info("NATS connections will authenticate with the NKey seed from '%s'".formatted(seedPath));
    }
}
