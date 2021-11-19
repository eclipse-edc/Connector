package org.eclipse.dataspaceconnector.dataloading;

class TestEntity {
    private final String description;
    private final int index;

    public TestEntity(String description, int index) {
        this.description = description;
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public int getIndex() {
        return index;
    }

}
