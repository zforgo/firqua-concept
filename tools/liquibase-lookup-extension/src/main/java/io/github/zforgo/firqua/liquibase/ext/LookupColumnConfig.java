package io.github.zforgo.firqua.liquibase.ext;

import liquibase.serializer.AbstractLiquibaseSerializable;

/**
 * A standard configuration class used by {@link InsertWithLookupChange} classes.
 * <p>
 * It describes a single {@code <lookupColumn/>} element: the row of {@link #getTable()} whose {@link #getKeyColumn()}
 * equals {@link #getKeyValue()} is looked up, and its {@link #getIdColumn()} is inserted into
 * {@link #getTargetColumn()}.
 */
public class LookupColumnConfig extends AbstractLiquibaseSerializable {

    private String table;
    private String keyColumn;
    private String idColumn = "id";
    private String keyValue;
    private String targetColumn;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

    @Override
    public String getSerializedObjectName() {
        return "lookupColumn";
    }

    @Override
    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }
}