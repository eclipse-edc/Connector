package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.negotiation.store.memory.TestFunctions.createNegotiation;

class ContractNegotiationPredicateConverterTest {
    private ContractNegotiationPredicateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ContractNegotiationPredicateConverter();
    }

    @Test
    void convert_nameEquals() {
        var criterion = new Criterion("id", "=", "test-cn");
        var n = createNegotiation("test-cn");
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(n)).isTrue();
    }

    @Test
    void convert_operatorIn() {
        var n = createNegotiation("test-cn");
        var criterion = new Criterion("id", "in", "(bob, test-cn)");
        var pred = converter.convert(criterion);
        assertThat(pred).isNotNull().accepts(n);

    }

    @Test
    void convert_invalidOperator() {
        var criterion = new Criterion("name", "GREATER_THAN", "(bob, alice)");
        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator [GREATER_THAN] is not supported by this converter!");

    }


}