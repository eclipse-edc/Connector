package com.microsoft.dagx.spi.types.domain.transfer;

/**
 *
 */
class TestResourceDefinition extends ResourceDefinition {

    public static class Builder extends ResourceDefinition.Builder<TestResourceDefinition, Builder> {
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TestResourceDefinition());
        }
    }
}
