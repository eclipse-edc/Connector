package org.eclipse.dataspaceconnector.ids.daps;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

public class Dat {
    private final String token;
    private final Instant expirationDate;

    public Dat(@NotNull final String token, @NotNull final Instant expirationDate) {
        Objects.requireNonNull(token);
        Objects.requireNonNull(expirationDate);

        this.token = token;
        this.expirationDate = expirationDate;
    }

    public String getToken() {
        return token;
    }

    public boolean isExpired() {
        return expirationDate.isAfter(Instant.now());
    }
}
