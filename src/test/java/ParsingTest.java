import com.intellij.testFramework.ParsingTestCase;
import com.javampire.openscad.parser.OpenSCADParserDefinition;

import java.io.IOException;

public class ParsingTest extends ParsingTestCase {
    public ParsingTest() {
        super("", "scad", new OpenSCADParserDefinition());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/openscad";
    }

    @Override
    protected boolean skipSpaces() {
        return false;
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }

    public void testDep1() throws IOException {
        doTest("_psidump");
    }

    public void testSubdep11() throws IOException {
        doTest("_psidump");
    }

    public void testMain() throws IOException {
        doTest("_psidump");
    }
}
