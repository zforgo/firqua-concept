package io.github.zforgo.firqua.liquibase.ext;

import java.io.Serial;

import liquibase.database.Database;
import liquibase.exception.UnexpectedLiquibaseException;

public class LookupNotFoundException extends UnexpectedLiquibaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public LookupNotFoundException(LookupColumnConfig lc, Database database) {
        super(
                "No result found in " + lc.getTable() + " with " + lc.getKeyColumn() + "=" + lc.getKeyValue() + " for "
                        + database
        );
    }
}
