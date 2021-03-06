package me.salzinger.intellij.coco;

import com.intellij.psi.tree.IElementType;
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
public class LexerKeywordTest extends AbstractLexerTest {
    private final String token;
    private final IElementType elementType;

    public LexerKeywordTest(String token, IElementType elementType) {
        this.token = token;
        this.elementType = elementType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"ANY", CocoTypes.KEYWORD_ANY},
                {"CHARACTERS", CocoTypes.KEYWORD_CHARACTERS},
                {"COMMENTS", CocoTypes.KEYWORD_COMMENTS},
                {"COMPILER", CocoTypes.KEYWORD_COMPILER},
                {"CONTEXT", CocoTypes.KEYWORD_CONTEXT},
                {"END", CocoTypes.KEYWORD_END},
                {"FROM", CocoTypes.KEYWORD_FROM},
                {"IF", CocoTypes.KEYWORD_IF},
                {"IGNORE", CocoTypes.KEYWORD_IGNORE},
                {"IGNORECASE", CocoTypes.KEYWORD_IGNORECASE},
                {"NESTED", CocoTypes.KEYWORD_NESTED},
                {"out", CocoTypes.KEYWORD_OUT},
                {"PRAGMAS", CocoTypes.KEYWORD_PRAGMAS},
                {"PRODUCTIONS", CocoTypes.KEYWORD_PRODUCTIONS},
                {"SYNC", CocoTypes.KEYWORD_SYNC},
                {"TO", CocoTypes.KEYWORD_TO},
                {"TOKENS", CocoTypes.KEYWORD_TOKENS},
                {"WEAK", CocoTypes.KEYWORD_WEAK}
        });
    }

    @Test
    public void testToken() throws IOException {
        init(token, CocoLexer.STATE_COMPILER);
        assertElementTypeStrict(elementType);
    }
}
