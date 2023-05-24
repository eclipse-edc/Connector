package org.eclipse.edc.sql.lease;

class TestEntityLeaseStatements implements LeaseStatements {

    @Override
    public String getDeleteLeaseTemplate() {
        return "DELETE FROM edc_lease WHERE lease_id=?;";
    }

    @Override
    public String getInsertLeaseTemplate() {
        return "INSERT INTO edc_lease (lease_id, leased_by, leased_at, lease_duration) VALUES (?, ?, ?, ?);";
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return "UPDATE " + getEntityTableName() + " SET lease_id=? WHERE id = ?;";
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return "SELECT * FROM edc_lease WHERE lease_id = (SELECT lease_id FROM " + getEntityTableName()
                + " WHERE id=?)";
    }

    public String getEntityTableName() {
        return "edc_test_entity";
    }
}
