package org.eclipse.edc.vault.hashicorp.auth;

public class HashicorpVaultTokenProviderImpl implements HashicorpVaultTokenProvider {

    private final String token;

    public HashicorpVaultTokenProviderImpl(String token) {
        this.token = token;
    }

    @Override
    public String vaultToken() {
        return token;
    }
}
