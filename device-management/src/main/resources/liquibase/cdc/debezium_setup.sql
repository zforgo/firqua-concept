--liquibase formatted sql

--changeset zforgo:cdc-role-create dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_roles WHERE rolname = '${dbz_user}'
CREATE ROLE "${dbz_user}" WITH LOGIN REPLICATION PASSWORD '${dbz_password}';

--changeset zforgo:cdc-role-sync runAlways:true runOnChange:true dbms:postgresql
ALTER ROLE "${dbz_user}" WITH LOGIN REPLICATION PASSWORD '${dbz_password}';

--changeset zforgo:cdc-grants runOnChange:true dbms:postgresql
GRANT USAGE ON SCHEMA public TO "${dbz_user}";
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "${dbz_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "${dbz_user}";
