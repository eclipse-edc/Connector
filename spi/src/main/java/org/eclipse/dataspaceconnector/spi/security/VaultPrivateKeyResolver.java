package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class VaultPrivateKeyResolver implements PrivateKeyResolver {


    private final Vault vault;
    private final List<KeyParser<?>> parsers;

    public VaultPrivateKeyResolver(Vault vault, KeyParser<?>... parsers) {
        this.vault = vault;
        this.parsers = Arrays.asList(parsers);
    }

    public VaultPrivateKeyResolver(Vault vault) {
        // can't use this(vault) here because properties are final
        this.vault = vault;
        parsers = new ArrayList<>();
    }

    @Override
    public @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType) {
        var encodedKey = vault.resolveSecret(id);

        if (encodedKey == null) {
            return null;
        }

        return keyType.cast(getParser(keyType).parse(encodedKey));
    }

    @Override
    public <T> void addParser(KeyParser<T> parser) {
        parsers.add(parser);
    }

    @Override
    public <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
        var p = new KeyParser<T>() {

            @Override
            public boolean canParse(Class<?> keyType) {
                return Objects.equals(keyType, forType);
            }

            @Override
            public T parse(String encoded) {
                return parseFunction.apply(encoded);
            }
        };
        addParser(p);
    }

    private <T> KeyParser<T> getParser(Class<T> keytype) {
        return (KeyParser<T>) parsers.stream().filter(p -> p.canParse(keytype))
                .findFirst().orElseThrow(() -> {
                            throw new EdcException("Cannot find KeyParser for type " + keytype);
                        }
                );
    }
}
