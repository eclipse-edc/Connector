package org.eclipse.dataspaceconnector.iam.ion;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IonClientImplTest {

    private static final String didUri = "did:ion:test:EiClWZ1MnE8PHjH6y4e4nCKgtKnI1DK1foZiP61I86b6pw";
    private static final String resolutionEndpoint = "http://23.97.144.59:3000/identifiers/";

    private IonClientImpl client;

    @BeforeEach
    void setup() {
        client = new IonClientImpl(resolutionEndpoint, new TypeManager());
    }

    @Test
    void resolve() {
        var result = client.resolve(didUri);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(didUri);
    }

    @Test
    void resolve_notFound() {
        assertThatThrownBy(() -> client.resolve("did:ion:test:notexist")).isInstanceOf(IonRequestException.class)
                .hasMessageContaining("404");
    }

    @Test
    void resolve_wrongPrefix() {
        assertThatThrownBy(() -> client.resolve("did:ion:foobar:notexist")).isInstanceOf(IonRequestException.class)
                .hasMessageContaining("400");
    }
}
