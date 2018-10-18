package com.github.mike10004.ormlitehelper.testtools;

import com.github.mike10004.common.dbhelp.H2MemoryConnectionSource;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Rule that creates a new H2 memory database on each test cycle.
 */
public class H2MemoryDatabaseContextRule extends DatabaseContextRule {

    private final Optional<Boolean> keepContentForLifeOfVmOption;
    private final Optional<String> schemaNameOption;

    public H2MemoryDatabaseContextRule(boolean keepContentForLifeOfVm, BookendOperation...bookendOperations) {
        this(keepContentForLifeOfVm, null, bookendOperations);
    }


    public H2MemoryDatabaseContextRule(BookendOperation...bookendOperations) {
        this(false, bookendOperations);
    }

    public H2MemoryDatabaseContextRule(boolean keepContentForLifeOfVm, @Nullable String schemaName, BookendOperation...bookendOperations) {
        super(bookendOperations);
        this.keepContentForLifeOfVmOption = Optional.of(keepContentForLifeOfVm);
        this.schemaNameOption = Optional.ofNullable(schemaName);
    }

    @Override
    protected H2MemoryConnectionSource createConnectionSource() {
        if (keepContentForLifeOfVmOption.isPresent() && schemaNameOption.isPresent()) {
            return new H2MemoryConnectionSource(schemaNameOption.get(), keepContentForLifeOfVmOption.get());
        } else if (keepContentForLifeOfVmOption.isPresent()) {
            return new H2MemoryConnectionSource(keepContentForLifeOfVmOption.get());
        } else {
            return new H2MemoryConnectionSource();
        }
    }

}