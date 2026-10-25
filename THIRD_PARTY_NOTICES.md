# Third-party software

The MIT license in this repository applies to the Lost and Found application
and its project documentation. Third-party software keeps its own license.

## MySQL Connector/J 26.10.0 Community

- Binary: `lib/mysql-connector-j-26.10.0.jar`
- Publisher: Oracle and/or its affiliates
- License: GNU GPL version 2, with the additional permissions and Universal
  FOSS Exception described in `lib/MYSQL-CONNECTOR-LICENSE.txt`.
- Original distribution notes: `lib/MYSQL-CONNECTOR-README.txt`.
- Corresponding source and original build files:
  `lib/mysql-connector-j-26.10.0-source.zip`.
- Official project: <https://dev.mysql.com/doc/connector-j/en/>

The binary is included so the documented plain-Java launch commands work
without a separate JDBC-driver download. The source archive is copied from
the same official Connector/J distribution; it is not application code and
does not need to be extracted to run this project.

Java and MySQL Server are installed separately and are not redistributed here.
