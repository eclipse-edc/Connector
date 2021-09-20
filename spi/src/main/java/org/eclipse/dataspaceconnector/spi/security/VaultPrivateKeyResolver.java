package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VaultPrivateKeyResolver implements PrivateKeyResolver {


    private final Vault vault;
    private final List<KeyParser> parsers;

    public VaultPrivateKeyResolver(Vault vault, List<KeyParser> parsers) {
        this.vault = vault;
        this.parsers = parsers;
    }

    @Override
    public @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType) {
        var encodedKey = vault.resolveSecret(id);

        return keyType.cast(getParser(keyType).parse(encodedKey));
    }

    public void addParser(KeyParser parser) {
        parsers.add(parser);
    }

    private <T> KeyParser getParser(Class<T> keytype) {
        return parsers.stream().filter(p -> p.canParse(keytype))
                .findFirst().orElseThrow(() -> {
                            throw new EdcException("Cannot find a Keyparser for type" + keytype);
                        }
                );
    }
}
