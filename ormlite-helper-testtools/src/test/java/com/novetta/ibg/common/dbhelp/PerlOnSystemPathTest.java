/*
 * Copyright 2015 Mike Chaberski.
 */
package com.novetta.ibg.common.dbhelp;

import com.novetta.ibg.common.sys.Whicher;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test that checks that perl executable is on the system path. This test checks 
 * whether the environment will allow the integration tests, which require
 * perl, to pass. If perl is not on the system path, the mysql integration 
 * test plugin will fail with an error message that is unhelpful for identifying 
 * the actual problem. This unit test, which runs before the integration test,
 * will print a more helpful error message instructing the user to install
 * perl.
 * 
 * @author mchaberski
 */
public class PerlOnSystemPathTest {
    
    @Test
    public void confirmPerlExectuableIsOnSystemPath() {
        System.out.println("confirmPerlExectuableIsOnSystemPath");
        Whicher whicher = Whicher.gnu();
        File perlExecutable = whicher.which("perl").orNull();
        if (perlExecutable == null || !perlExecutable.isFile()) {
            printErrorMessage();
            fail("perl executable not found in any of these directories: " + System.getenv("PATH"));
            return;
        }
        if (!perlExecutable.canExecute()) {
            System.err.println("The perl executable was found at " 
                    + perlExecutable.getAbsolutePath() 
                    + ", but that file is not executable.");
            fail("perl is not executable: " + perlExecutable);
        }
        System.out.println("which perl: " + perlExecutable);
    }
    
    private void printErrorMessage() {
        System.err.println("The integration tests for this project use a "
                + "MySQL plugin that executes perl scripts. "
                + "The perl executable must be found in one of the "
                + "directories specified by the system PATH environment "
                + "variable. On Windows, you can get perl from "
                + "http://strawberryperl.com. Install it and make sure "
                + "the directory containing perl.exe is one of the "
                + "directories in the PATH environment variable. If perl"
                + "is not installed and cannot be installed, you must"
                + "skip the unit and integration tests.");
    }
}
