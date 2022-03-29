package org.eclipse.dataspaceconnector.sql.asset.index;

public interface SqlAssetQueries extends SqlAssetTables {

    String getSqlAssetInsertClause();

    String getSqlDataAddressInsertClause();

    String getSqlPropertyInsertClause();

    String getSqlAssetCountByIdClause();

    String getSqlPropertyFindByIdClause();

    String getSqlDataAddressFindByIdClause();

    String getSqlAssetListClause();

    String getSqlAssetDeleteByIdClause();

    String getSqlDataAddressDeleteByIdClause();

    String getSqlPropertyDeleteByIdClause();

    String getCountVariableName();
}
