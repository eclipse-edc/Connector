package org.eclipse.dataspaceconnector.sql.asset.index;


public class PostgresSqlAssetQueries implements SqlAssetQueries {

    private static final String ASSET_TABLE = "edc_asset";
    private static final String ASSET_COLUMN_ID = "asset_id";

    private static final String DATA_ADDRESS_TABLE = "edc_asset_dataaddress";
    private static final String DATA_ADDRESS_COLUMN_PROPERTIES = "properties";

    private static final String ASSET_PROPERTY_TABLE = "edc_asset_property";
    private static final String ASSET_PROPERTY_COLUMN_NAME = "property_name";
    private static final String ASSET_PROPERTY_COLUMN_VALUE = "property_value";
    private static final String ASSET_PROPERTY_COLUMN_TYPE = "property_type";

    private static final String COUNT_VARIABLE_NAME = "count";

    private static final String SQL_ASSET_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s) VALUES (?)",
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID,
            DATA_ADDRESS_COLUMN_PROPERTIES);
    private static final String SQL_PROPERTY_INSERT_CLAUSE_TEMPLATE = String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID,
            ASSET_PROPERTY_COLUMN_NAME,
            ASSET_PROPERTY_COLUMN_VALUE,
            ASSET_PROPERTY_COLUMN_TYPE);

    private static final String SQL_ASSET_COUNT_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT COUNT(*) AS %s FROM %s WHERE %s = ?",
            COUNT_VARIABLE_NAME,
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_PROPERTY_FIND_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s WHERE %s = ?",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_FIND_BY_ID_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s WHERE %s = ?",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_LIST_CLAUSE_TEMPLATE = String.format("SELECT * FROM %s",
            ASSET_TABLE);

    private static final String SQL_ASSET_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            ASSET_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_DATA_ADDRESS_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            DATA_ADDRESS_TABLE,
            ASSET_COLUMN_ID);
    private static final String SQL_ASSET_PROPERTY_DELETE_BY_ID_CLAUSE_TEMPLATE = String.format("DELETE FROM %s WHERE %s = ?",
            ASSET_PROPERTY_TABLE,
            ASSET_COLUMN_ID);

    @Override
    public String getSqlAssetInsertClause() {
        return SQL_ASSET_INSERT_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlDataAddressInsertClause() {
        return SQL_DATA_ADDRESS_INSERT_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlPropertyInsertClause() {
        return SQL_PROPERTY_INSERT_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlAssetCountByIdClause() {
        return SQL_ASSET_COUNT_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlPropertyFindByIdClause() {
        return SQL_ASSET_PROPERTY_FIND_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlDataAddressFindByIdClause() {
        return SQL_DATA_ADDRESS_FIND_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlAssetListClause() {
        return SQL_ASSET_LIST_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlAssetDeleteByIdClause() {
        return SQL_ASSET_DELETE_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlDataAddressDeleteByIdClause() {
        return SQL_DATA_ADDRESS_DELETE_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getSqlPropertyDeleteByIdClause() {
        return SQL_ASSET_PROPERTY_DELETE_BY_ID_CLAUSE_TEMPLATE;
    }

    @Override
    public String getCountVariableName() {
        return COUNT_VARIABLE_NAME;
    }

    @Override
    public String getAssetTable() {
        return ASSET_TABLE;
    }

    @Override
    public String getAssetColumnId() {
        return ASSET_COLUMN_ID;
    }

    @Override
    public String getDataAddressTable() {
        return DATA_ADDRESS_TABLE;
    }

    @Override
    public String getDataAddressColumnProperties() {
        return DATA_ADDRESS_COLUMN_PROPERTIES;
    }

    @Override
    public String getAssetPropertyTable() {
        return ASSET_PROPERTY_TABLE;
    }

    @Override
    public String getAssetPropertyColumnName() {
        return ASSET_PROPERTY_COLUMN_NAME;
    }

    @Override
    public String getAssetPropertyColumnValue() {
        return ASSET_PROPERTY_COLUMN_VALUE;
    }

    @Override
    public String getAssetPropertyColumnType() {
        return ASSET_PROPERTY_COLUMN_TYPE;
    }
}
