/**
 * (c) 2016 Mike Chaberski. Distributed under terms of the MIT License.
 */
package com.github.mike10004.ormlitehelper.testtools;

import com.novetta.ibg.common.dbhelp.H2MemoryConnectionSource;

/**
 *
 * @author mchaberski
 */
public class H2MemoryDatabaseContextRule extends DatabaseContextRule {

    public H2MemoryDatabaseContextRule(SetupOperation...setupActions) {
        super(setupActions);
    }

    @Override
    protected H2MemoryConnectionSource createConnectionSource() {
        return new H2MemoryConnectionSource();
    }

}