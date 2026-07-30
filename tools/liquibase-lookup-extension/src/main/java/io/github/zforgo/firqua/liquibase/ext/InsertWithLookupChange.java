package io.github.zforgo.firqua.liquibase.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import liquibase.Scope;
import liquibase.change.ChangeMetaData;
import liquibase.change.ColumnConfig;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.change.core.InsertDataChange;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawParameterizedSqlStatement;

@DatabaseChange(name = "insertWithLookup",
        description = "Insert with FK resolved from a natural-key lookup table",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = "table")
public class InsertWithLookupChange extends InsertDataChange {

    private static final Map<String, Map<String, Object>> cache = new HashMap<>();
    private final List<LookupColumnConfig> lookupColumns = new ArrayList<>();

    public void addLookupColumn(LookupColumnConfig c) {
        lookupColumns.add(c);
    }

    @DatabaseChangeProperty(description = "Foreign key columns resolved from a natural key of the referenced table",
            requiredForDatabase = "all")
    public List<LookupColumnConfig> getLookupColumns() {
        return lookupColumns;
    }

    /**
     * Redeclared only to carry the annotation: {@code requiredForDatabase} defaults to
     * {@link liquibase.change.ChangeParameterMetaData#COMPUTE}, and the computation is skipped for changes that are
     * {@link #generateStatementsVolatile(Database) volatile}, which would leave the table name unvalidated.
     */
    @Override
    @DatabaseChangeProperty(mustEqualExisting = "table",
            description = "Name of the table to insert data into",
            requiredForDatabase = "all")
    public String getTableName() {
        return super.getTableName();
    }

    /**
     * Only a property with both a getter and a setter becomes a {@link ChangeMetaData} parameter, and only parameters
     * are populated from the changelog. Without this setter the {@code <lookupColumn/>} elements are silently dropped.
     */
    @SuppressWarnings("unused")
    public void setLookupColumns(List<LookupColumnConfig> lookupColumns) {
        this.lookupColumns.clear();
        if (lookupColumns != null) {
            this.lookupColumns.addAll(lookupColumns);
        }
    }

    /**
     * The statements are built from live database content, so they cannot be generated before the earlier changesets
     * have run. Without this, changelog validation would query the lookup table before it even exists.
     */
    @Override
    public boolean generateStatementsVolatile(Database database) {
        return true;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        for (var lc : lookupColumns) {
            var cacheKey = database.getConnection().getURL() + "|" + lc.getTable() + "." + lc.getKeyColumn();
            var map = cache.computeIfAbsent(cacheKey, _ -> loadLookupMap(database, lc));

            var resolved = Optional.ofNullable(map.get(lc.getKeyValue()))
                    .orElseThrow(() -> new LookupNotFoundException(lc, database));

            var resolvedColumn = new ColumnConfig();
            resolvedColumn.setName(lc.getTargetColumn());
            resolvedColumn.setValueNumeric((Number) resolved);
            this.addColumn(resolvedColumn);
        }
        return super.generateStatements(database);
    }

    private Map<String, Object> loadLookupMap(Database database, LookupColumnConfig lc) {
        var sql = "SELECT " + lc.getKeyColumn() + ", " + lc.getIdColumn() + " FROM " + lc.getTable();
        try {
            final var executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", database);
            final var rows = executor.queryForList(new RawParameterizedSqlStatement(sql));

            final var lookup = new HashMap<String, Object>();
            for (var row : rows) {
                final var byColumnName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                byColumnName.putAll(row);
                lookup.put(String.valueOf(byColumnName.get(lc.getKeyColumn())), byColumnName.get(lc.getIdColumn()));
            }
            return lookup;
        } catch (DatabaseException e) {
            Scope.getCurrentScope().getLog(InsertWithLookupChange.class).severe("Failed to load lookup map from database.", e);
            throw new UnexpectedLiquibaseException(e);
        }
    }
}
