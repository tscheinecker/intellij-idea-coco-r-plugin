package at.jku.ssw.coco.intellij.reference;

import at.jku.ssw.coco.intellij.CocoUtil;
import at.jku.ssw.coco.intellij.psi.HasCocoCompilerReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CocoCompilerReference extends AbstractRenamableReference<HasCocoCompilerReference> implements PsiReference {

    public CocoCompilerReference(@NotNull HasCocoCompilerReference element, TextRange textRange) {
        super(element, textRange);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return CocoUtil.findCompiler(myElement.getContainingFile(), myElement.getName());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return CocoUtil.findCompilers(myElement.getContainingFile()).toArray();
    }
}