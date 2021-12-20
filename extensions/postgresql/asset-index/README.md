# PostgreSQL - Asset Index

## Configuration

### Connection

| NAME                    | KEY                                  | Type    | Description                                                                                                                                                                                   |
|:------------------------|:-------------------------------------|:--------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Url                     | edc.postgresql.url                   | URI     | The url of the PostgreSQL database. If settings are specified in the URL, too, the values from other settings are ignored.                                                                    |
| Username                | edc.postgresql.username              | String  | The database user.                                                                                                                                                                            |
| Password                | edc.postgresql.password              | String  | The password of the database user.                                                                                                                                                            |                                                                                         |
| SSL                     | edc.postgresql.ssl                   | Boolean | If true the connection is made using SSL.                                                                                                                                                     |
| SSL Mode                | edc.postgresql.sslMode               | String  | Must be ether 'disable', 'allow', 'prefer', 'require', 'verify-ca' or 'verify-full'.                                                                                                          |
| SSL Certificate         | edc.postgresql.sslCert               | Path    | Full path for a certificate file. Must be PEM encoded X509v3 certificate.                                                                                                                     |
| SSL Key                 | edc.postgresql.sslKey                | Path    | Full path for a key file. Must be in [PKCS-12](https://en.wikipedia.org/wiki/PKCS_12) or [PKCS-8](https://en.wikipedia.org/wiki/PKCS_8) [DER format](https://wiki.openssl.org/index.php/DER). |
| SSL Root Cert           | edc.postgresql.sslRootCert           | String  | File name of the SSL root cert                                                                                                                                                                |
| Host Name Verifier      | edc.postgresql.ssl.hostnameVerifier  | String  | Class name of host name verifier.                                                                                                                                                             |
| Logger Level            | edc.postgresql.loggerLevel           | String  | Must be ether 'OFF', 'DEBUG' or 'TRACE'                                                                                                                                                       |
| Logger File             | edc.postgresql.loggerFile            | String  | File name output of the logger.                                                                                                                                                               |
| Log Unclosed Connection | edc.postgresql.logUnclosedConnection | Boolean | True to log leaked connections, that didn't call the `close()` method.                                                                                                                        |
| Socket Timeout          | edc.postgresql.socketTimeout         | Integer | Timeout value for socket read operations in seconds.                                                                                                                                          |
| Connection Timeout      | edc.postgresql.connectionTimeout     | Integer | Timeout value for socket connect operations in seconds.                                                                                                                                       |
| Login Timeout           | edc.postgresql.loginTimeout          | Integer | Timeout to establish a database connection in seconds.                                                                                                                                        |
| Application Name        | edc.postgresql.applicationName       | String  | Name of the application, that is using the connection.                                                                                                                                        |
| Read Only               | edc.postgresql.readOnly              | Boolean | Put the connection in read-only mode.                                                                                                                                                         |

For a more detailed description about the different settings, please have a look at the official documentation of the
PostgreSQL JDBC driver. https://jdbc.postgresql.org/documentation/head/connect.html

### Connection Pool

| NAME                       | KEY                                                        | Type    | Description                                                           |
|:---------------------------|:-----------------------------------------------------------|:--------|:----------------------------------------------------------------------|
| Max. Idle Connections      | edc.postgresql.connection.pool.max.idle.connections        | Integer | Maximum number of connections that can remain idle in the pool.       |
| Min. Idle Connections      | edc.postgresql.connection.pool.min.idle.connections        | Integer | Minimum number of connections that can remain idle in the pool.       |
| Max Total Connections      | edc.postgresql.connection.pool.max.total.connections       | Integer | Maximum number of connections that can be allocated at the same time. |
| Test Connection on Borrow  | edc.postgresql.connection.pool.test.connection.on.borrow   | Boolean | True to validate a connection before it's borrowed from the pool.     |
| Test Connection on Create  | edc.postgresql.connection.pool.test.connection.on.create   | Boolean | True to validate a connection after its creation.                     |
| Test Connection on Return  | edc.postgresql.connection.pool.test.connection.on.return   | Boolean | True to validate a connection after it is returned to the pool.       |
| Test Connection while Idle | edc.postgresql.connection.pool.test.connection.while.idle  | Boolean | True to validate connections while they are idle.                     |
| Test Connection Query      | edc.postgresql.connection.pool.test.connection.query       | String  | Query that is used to validate connections. Default `SELECT 1;`.      |
