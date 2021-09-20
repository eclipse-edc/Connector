package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.interfaces.DHPrivateKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.spi.security.PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER;
import static org.eclipse.dataspaceconnector.spi.security.PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM;

class VaultPrivateKeyResolverTest {

    private static final String TEST_SECRET_ALIAS = "test-secret";
    private Vault vault;
    private VaultPrivateKeyResolver resolver;

    @BeforeEach
    void setUp() {
        vault = niceMock(Vault.class);
        resolver = new VaultPrivateKeyResolver(vault);
        resolver.addParser(new DummyParser());
    }

    @Test
    void resolvePrivateKey() {
        expect(vault.resolveSecret(TEST_SECRET_ALIAS)).andReturn(ENCODED_PRIVATE_KEY_HEADER);
        replay(vault);
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();
        verify(vault);
    }

    @Test
    void resolvePrivateKey_secretNotFound() {
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNull();
    }

    @Test
    void resolvePrivateKey_secretNotInCorrectFormat() {
        expect(vault.resolveSecret(TEST_SECRET_ALIAS)).andReturn(ENCODED_PRIVATE_KEY_NOPEM);
        replay(vault);

        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(IllegalArgumentException.class);
        verify(vault);
    }

    @Test
    void resolvePrivateKey_noParserFound() {
        expect(vault.resolveSecret(TEST_SECRET_ALIAS)).andReturn(ENCODED_PRIVATE_KEY_NOPEM);
        replay(vault);

        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, DHPrivateKey.class)).isInstanceOf(EdcException.class)
                .hasMessageStartingWith("Cannot find KeyParser for type");
        verify(vault);
    }

    @Test
    void addParser() {
        expect(vault.resolveSecret(TEST_SECRET_ALIAS)).andReturn(ENCODED_PRIVATE_KEY_HEADER).times(2);
        replay(vault);

        resolver = new VaultPrivateKeyResolver(vault);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(new DummyParser());

        //same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();

        verify(vault);

    }

    @Test
    void testAddParser() {
        expect(vault.resolveSecret(TEST_SECRET_ALIAS)).andReturn(ENCODED_PRIVATE_KEY_HEADER).times(2);
        replay(vault);

        resolver = new VaultPrivateKeyResolver(vault);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(RSAPrivateKey.class, s -> new DummyParser().parse(s));

        //same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();

        verify(vault);
    }

    private static class DummyParser implements KeyParser<RSAPrivateKey> {

        private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
        private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";

        @Override
        public boolean canParse(Class<?> keyType) {
            return keyType.equals(RSAPrivateKey.class);
        }

        @Override
        public RSAPrivateKey parse(String entirePemFileContent) {
            entirePemFileContent = entirePemFileContent.replace(PEM_HEADER, "").replaceAll(System.lineSeparator(), "").replace(PEM_FOOTER, "");
            entirePemFileContent = entirePemFileContent.replace("\n", ""); //base64 might complain if newlines are present

            KeyFactory keyFactory = null;
            try {
                keyFactory = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(entirePemFileContent.getBytes())));

            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

