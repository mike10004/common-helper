/**
 * (c) 2016 Mike Chaberski. Distributed under terms of the MIT License.
 */
package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.base.Optional;
import com.github.mike10004.common.dbhelp.H2MemoryConnectionSource;

import javax.annotation.Nullable;

/**
 *
 * @author mchaberski
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
        this.schemaNameOption = Optional.fromNullable(schemaName);
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