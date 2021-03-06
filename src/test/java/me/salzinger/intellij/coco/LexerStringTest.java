package me.salzinger.intellij.coco;

import me.salzinger.intellij.coco.psi.CocoTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by Thomas on 23/03/2015.
 */
@RunWith(Parameterized.class)
public class LexerStringTest extends AbstractLexerTest {
    private final String string;

    public LexerStringTest(String string) {
        this.string = string;
    }

    @Parameterized.Parameters
    public static Collection<String[]> data() {
        String[][] strings = {
                {"\"\""},
                {"\"\\'\""},
                {"\"simpleString\""},
                {"\"string with spaces\""},
                {"\"string with ' apostrophe\""},
                {"\"string with \\\" quote\""},
                {"\"string with \\n new line\""},
                {"\"string with \\r carriage return\""},
                {"\"string with \\0 null character \""},
                {"\"string with \\a bell\""},
                {"\"string with \\b backspace\""},
                {"\"string with \\t horizontal tab\""},
                {"\"string with \\v vertical tab\""},
                {"\"string with \\f form feed\""},
                {"\"string with \\\\ backslash \""},
                {"\"string with \\u0DF3 hex char value \""}
        };
        return Arrays.asList(strings);
    }

    @Test
    public void testString() throws IOException {
        init(string, CocoLexer.STATE_COMPILER);
        assertElementTypeStrict(CocoTypes.STRING);
    }
}
