package at.jku.ssw.coco.intellij.psi;

import at.jku.ssw.coco.intellij.CocoLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Thomas Scheinecker <a href="mailto:tscheinecker@gmail.com">tscheinecker@gmail.com</a>
 */
public class CocoElementType extends IElementType {
    public CocoElementType(@NotNull @NonNls String debugName) {
        super(debugName, CocoLanguage.INSTANCE);
    }
}
