package com.github.mike10004.ormlitehelper.testtools;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.novetta.ibg.common.dbhelp.H2MemoryConnectionSource;
import com.novetta.ibg.common.sys.Platforms;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

/**
 * Created by mchaberski on 1/20/16.
 */
public class H2Test {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testTransfer() throws Exception {
        System.out.println("testTransfer");
        H2MemoryConnectionSource cs1 = new H2MemoryConnectionSource(true);
        TableUtils.createTable(cs1, Widget.class);
        Dao<Widget, ?> dao1 = DaoManager.createDao(cs1, Widget.class);
        dao1.create(new Widget("green"));

        H2MemoryConnectionSource cs2 = new H2MemoryConnectionSource(true);
        cs2.forcePrepareAndInitialize();
        String url1 = cs1.getUrl(), url2 = cs2.getUrl();
        checkState(url1 != null, "url1 null");
        checkState(url2 != null, "url2 null");
        H2.transfer(url1, url2, temporaryFolder.getRoot());

        Dao<Widget, ?> dao2 = DaoManager.createDao(cs2, Widget.class);
        List<Widget> widgets2 = dao2.queryForAll();
        System.out.println("widgets in transferred connection: " + widgets2);
        assertEquals(1, widgets2.size());
        assertEquals("green", widgets2.get(0).color);
    }

    @Test
    public void testFileUrlBuilder() throws Exception {
        System.out.println("testFileUrlBuilder");
        if (Platforms.getPlatform().isWindows()) {
            System.out.println("skipping test because this is Windows");
            return;
        }
        File dbFile = new File("/path/to/goodfile.h2.db");
        String url = new H2.H2FileUrlBuilder(dbFile).build();
        System.out.format("%s -> %s%n", dbFile, url);
        assertEquals("jdbc:h2:file:/path/to/goodfile", url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileUrlBuilder_badName1() throws Exception {
        System.out.println("testFileUrlBuilder_badName1");
        new H2.H2FileUrlBuilder(new File("/path/to/badname.txt")).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFileUrlBuilder_badName2() throws Exception {
        System.out.println("testFileUrlBuilder_badName2");
        new H2.H2FileUrlBuilder(new File("/path/to/badname")).build();
    }

    @Test
    public void testMemoryUrlBuilder_default() throws Exception {
        System.out.println("testMemoryUrlBuilder");
        String noCustomization = new H2.H2MemoryUrlBuilder().build();
        System.out.println("no customization: " + noCustomization);
        assertTrue("mismatch on URL with no customization", noCustomization.matches("jdbc:h2:mem:[A-Za-z][A-Za-z0-9_]{36}"));
    }

    @Test
    public void testMemoryUrlBuilder_keepContent() throws Exception {
        System.out.println("testMemoryUrlBuilder_keepContent");
        String keepContent = new H2.H2MemoryUrlBuilder().setKeepContentForLifeOfVM(true).build();
        System.out.println("keep content: " + keepContent);
        assertTrue(keepContent.matches("jdbc:h2:mem:[A-Za-z][A-Za-z0-9_]{36}\\Q;DB_CLOSE_DELAY=-1\\E"));
    }

    @Test
    public void testMemoryUrlBuilder_customSchema() throws Exception {
        System.out.println("testMemoryUrlBuilder_customSchema");
        String customSchema = new H2.H2MemoryUrlBuilder().setSchema("obiwankenobi").build();
        System.out.println("custom schema: " + customSchema);
        assertEquals("jdbc:h2:mem:obiwankenobi", customSchema);
    }

    @Test
    public void testMemoryUrlBuilder_customSchema_keepContent() throws Exception {
        System.out.println("testMemoryUrlBuilder_customSchema_keepContent");
        String customSchemaKeepContent = new H2.H2MemoryUrlBuilder().setSchema("obiwankenobi").setKeepContentForLifeOfVM(true).build();
        System.out.println("custom schema, keep content: " + customSchemaKeepContent);
        assertEquals("jdbc:h2:mem:obiwankenobi;DB_CLOSE_DELAY=-1", customSchemaKeepContent);
    }

}