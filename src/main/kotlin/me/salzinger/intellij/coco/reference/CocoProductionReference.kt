package me.salzinger.intellij.coco.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import me.salzinger.intellij.coco.findByName
import me.salzinger.intellij.coco.findProductions
import me.salzinger.intellij.coco.psi.HasCocoProductionReference

class CocoProductionReference(element: HasCocoProductionReference, textRange: TextRange) :
    AbstractRenamableReference<HasCocoProductionReference>(element, textRange), PsiReference {

    override fun resolve(): PsiElement? {
        return findProductions(myElement.containingFile).findByName(myElement.name)
    }

    override fun getVariants(): Array<Any> {
        return findProductions(myElement.containingFile)
            .map { it ->
                val formalAttributes = it.formalAttributes
                val lookupElementBuilder = LookupElementBuilder.create(it)
                    .withTypeText("Production")

                if (formalAttributes != null) {
                    return@map lookupElementBuilder.withTailText(formalAttributes.text, true)
                }

                lookupElementBuilder
            }
            .toTypedArray()
    }
}
