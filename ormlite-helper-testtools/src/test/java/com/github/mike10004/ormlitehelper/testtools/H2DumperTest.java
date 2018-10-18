package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.github.mike10004.common.dbhelp.H2MemoryConnectionSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.*;

public class H2DumperTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testAllCompressionsAndOptions() throws Exception {
        System.out.println("testAllCompressionsAndOptions");
        H2MemoryConnectionSource cs = new H2MemoryConnectionSource(true);
        TableUtils.createTable(cs, Widget.class);
        DaoManager.createDao(cs, Widget.class).create(new Widget("yellow"));
        String jdbcUrl = cs.getUrl();
        final Boolean[] withTableValues = {null, false, true};
        try {
            for (H2.Dumper.Compression compression : H2.Dumper.Compression.values()) {
                for (Set<H2.Dumper.DumpOption> options : Sets.powerSet(ImmutableSet.copyOf(H2.Dumper.DumpOption.values()))) {
                    for (Boolean withTable : withTableValues) {
                        H2.Dumper dumper = new H2.Dumper(Charsets.UTF_8, compression);
                        for (H2.Dumper.DumpOption option : options) {
                            dumper.setOption(option);
                        }
                        System.out.format("compression = %s, options = %s, withTable = %s%n", compression, options, withTable);
                        if (withTable != null) {
                            if (withTable.booleanValue()) {
                                dumper.setTables("widget");
                            } else {
                                dumper.setTables("widget"); // first set it
                                dumper.allTables(); // then remove it
                            }
                        }
                        File dir = temp.newFolder();
                        File dumpFile = new File(dir, "dumped.h2.sql");
                        dumper.dump(jdbcUrl, null, null, dumpFile);
                        System.out.format("%d bytes in %s%n", dumpFile.length(), dumpFile);
                        assertTrue("dumpfile not empty", dumpFile.length() > 0);
                    }
                }
            }
        } finally {
            cs.close();
        }

    }

}