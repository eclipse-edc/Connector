package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseCriterionToPredicateConverterTest {

    private TestCriterionToPredicateConverter converter = new TestCriterionToPredicateConverter();

    @Test
    void convertEqual() {
        var predicate = converter.convert(new Criterion("value", "=", "any"));

        assertThat(predicate)
                .accepts(new TestObject("any"))
                .rejects(new TestObject("other"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertIn() {
        var predicate = converter.convert(new Criterion("value", "in", "(first, second)"));

        assertThat(predicate)
                .accepts(new TestObject("first"), new TestObject("second"))
                .rejects(new TestObject("third"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertLike() {
        var predicate = converter.convert(new Criterion("value", "like", "any"));

        assertThat(predicate)
                .accepts(new TestObject("any"))
                .rejects(new TestObject("other"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertLikePrefix() {
        var predicate = converter.convert(new Criterion("value", "like", "prefix%"));

        assertThat(predicate)
                .accepts(new TestObject("prefix"), new TestObject("prefix-suffix"))
                .rejects(new TestObject("other-prefix"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertLikeSuffix() {
        var predicate = converter.convert(new Criterion("value", "like", "%suffix"));

        assertThat(predicate)
                .accepts(new TestObject("suffix"), new TestObject("prefix-suffix"))
                .rejects(new TestObject("suffix-other"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertLikeContent() {
        var predicate = converter.convert(new Criterion("value", "like", "%content%"));

        assertThat(predicate)
                .accepts(new TestObject("content"), new TestObject("prefix-content-suffix"))
                .rejects(new TestObject("conten"), new TestObject(""), new TestObject(null));
    }

    private static class TestCriterionToPredicateConverter extends BaseCriterionToPredicateConverter<TestObject> {

        @Override
        protected <R> R property(String key, Object object) {
            return ReflectionUtil.getFieldValueSilent(key, object);
        }
    }

    private static class TestObject {
        @Override
        public String toString() {
            return "TestObject{" +
                    "value='" + value + '\'' +
                    '}';
        }

        private final String value;

        private TestObject(String value) {
            this.value = value;
        }
    }
}