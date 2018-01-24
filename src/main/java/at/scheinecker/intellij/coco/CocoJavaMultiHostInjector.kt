package at.scheinecker.intellij.coco

import at.scheinecker.intellij.coco.psi.CocoCocoInjectorHost
import at.scheinecker.intellij.coco.psi.CocoFactor
import at.scheinecker.intellij.coco.psi.CocoTokenDecl
import at.scheinecker.intellij.coco.settings.CocoConfiguration
import at.scheinecker.intellij.coco.settings.CocoInjectionMode
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil

class CocoJavaMultiHostInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (CocoConfiguration.getSettings(context.project).injectionMode != CocoInjectionMode.ADVANCED) return
        if (context !is CocoCocoInjectorHost) return

        val registrarAdapter = LazyMultiHostRegistrarAdapter(registrar, context)
        performInjection(registrarAdapter, context)
    }

    private fun performInjection(registrar: LazyMultiHostRegistrarAdapter, context: CocoCocoInjectorHost) {

        val psiFile = context.containingFile
        var prefixBuilder = StringBuilder()
        val targetPackage = CocoUtil.getTargetPackage(psiFile).map { "package $it;\n\n" }.orElse("")
        prefixBuilder.append(targetPackage)

        registrar.startInjecting(JavaLanguage.INSTANCE)

        val cocoImports = context.imports
        val globalFieldsAndMethods = context.globalFieldsAndMethods
        val scannerSpecification = context.scannerSpecification
        val pragmas = scannerSpecification.pragmas
        val productions = context.parserSpecification.productionList

        if (cocoImports != null && cocoImports.textLength != 0) {
            registrar.addPlace(targetPackage, null, context, TextRange.from(cocoImports.textOffset, cocoImports.textLength))
            prefixBuilder = StringBuilder()
        }

        prefixBuilder.append("class Parser {\n")
        prefixBuilder.append("\tvoid Recover() {/* generated */}\n")
        prefixBuilder.append("\tpublic static final int _EOF = 0;\n")
        appendSetDecls(prefixBuilder, scannerSpecification.tokens?.tokenDeclList.orEmpty())
        appendSetDecls(prefixBuilder, pragmas?.pragmaDeclList?.map { it.tokenDecl }.orEmpty(), 102)
        if (globalFieldsAndMethods != null && globalFieldsAndMethods.textLength != 0) {

            val start = globalFieldsAndMethods.textOffset
            registrar.addPlace(prefixBuilder.toString(), null, context, TextRange(start, globalFieldsAndMethods.nextSibling?.textOffset
                    ?: context.textLength))
            prefixBuilder = StringBuilder()
        }

        prefixBuilder.append("\n\nvoid Get() {\n")

        pragmas?.pragmaDeclList?.mapNotNull { it.semAction?.arbitraryStatements }?.forEach {
            val start = it.textOffset
            registrar.addPlace(prefixBuilder.toString(), null, context, TextRange(start, it.nextSibling?.textOffset
                    ?: context.textLength))
            prefixBuilder = StringBuilder()
        }

        prefixBuilder.append("}\n")

        productions.forEach {
            //            val start = it.textOffset
            prefixBuilder.append("\n")
            var parameterDeclaration = it.formalAttributes?.formalInputAttributes
            val formalOutputAttribute = it.formalAttributes?.formalAttributesWithOutput

            if (formalOutputAttribute != null) {
                parameterDeclaration = formalOutputAttribute.formalInputAttributes
                val formalAttributesParameter = formalOutputAttribute.formalOutputAttribute.formalAttributesParameter
                if (formalAttributesParameter != null) {
                    prefixBuilder.append(formalAttributesParameter.javaTypeReferenceList.first().text)
                    prefixBuilder.append(" ")
                } else {
                    prefixBuilder.append("void ")
                }
            } else {
                prefixBuilder.append("void ")
            }

            prefixBuilder.append("${it.name}(")
            if (parameterDeclaration != null) {
                registrar.addPlace(prefixBuilder.toString(), null, context, TextRange.from(parameterDeclaration.textOffset, parameterDeclaration.textLength))
                prefixBuilder = StringBuilder()
            }
            prefixBuilder.append(") {\n")


            if (formalOutputAttribute != null) {
                val formalAttributesParameter = formalOutputAttribute.formalOutputAttribute.formalAttributesParameter
                if (formalAttributesParameter != null) {
                    prefixBuilder.append("\t")
                    registrar.addPlace(prefixBuilder.toString(), ";\n", formalAttributesParameter)
                    prefixBuilder = StringBuilder()
                }
            }


            val localDeclaration = it.semAction?.arbitraryStatements
            if (localDeclaration != null) {
                prefixBuilder.append("\t")
                registrar.addPlace(prefixBuilder.toString(), "\n\tRecover();\n", context, TextRange.from(localDeclaration.textOffset, localDeclaration.textLength))
                prefixBuilder = StringBuilder()
            }

            val factors = PsiTreeUtil.findChildrenOfType(it, CocoFactor::class.java)

            factors.forEach {
                val ident = it.ident // != null -> method call
                if (ident != null) {
                    val parameters = it.actualAttributes?.actualAttributesBody // != null -> method params

                    val attributeAssignment = parameters?.attributeAssignment
                    prefixBuilder.append("\t")

                    if (parameters != null) {
                        val varName = attributeAssignment?.javaTypeReference
                        val attributeAssignmentLength = attributeAssignment?.textLength ?: 0
                        val additionalOffset = parameters.text.substring(attributeAssignmentLength).takeWhile { it == ' ' || it == '\t' || it == '\n' || it == '\n' || it == ',' }.length

                        if (varName != null) {
                            registrar.addPlace(prefixBuilder.toString(), " = ", varName)
                            prefixBuilder = StringBuilder()
                        }

                        prefixBuilder.append("${ident.text}(")

                        registrar.addPlace(
                                prefixBuilder.toString(),
                                ");\n",
                                context,
                                TextRange.from(
                                        parameters.textOffset + attributeAssignmentLength + additionalOffset,
                                        parameters.textLength - attributeAssignmentLength - additionalOffset
                                )
                        )
                        prefixBuilder = StringBuilder()
                    }
                }

                val arbitraryStatements = it.semAction?.arbitraryStatements
                if (arbitraryStatements != null) {
                    prefixBuilder.append("\t")
                    registrar.addPlace(prefixBuilder.toString(),  "\n\tRecover();\n", arbitraryStatements)
                    prefixBuilder = StringBuilder()
                }

            }

            if (formalOutputAttribute != null) {
                val formalAttributesParameter = formalOutputAttribute.formalOutputAttribute.formalAttributesParameter
                if (formalAttributesParameter != null && formalAttributesParameter.javaTypeReferenceList.size >= 2) {
                    prefixBuilder.append("return ")
                    prefixBuilder.append(formalAttributesParameter.javaTypeReferenceList.get(1).text)
                    prefixBuilder.append(";\n")
                }
            }

            prefixBuilder.append("}\n")

        }

        registrar.addPlace(prefixBuilder.toString(), "}", context, TextRange.from(context.textLength, 0))

        registrar.doneInjecting()
    }


    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        return mutableListOf(CocoCocoInjectorHost::class.java)
    }

    class LazyMultiHostRegistrarAdapter(val registrar: MultiHostRegistrar, val conetxt: PsiLanguageInjectionHost) : MultiHostRegistrar {
        var language: Language? = null
        var started = false
        val injected = StringBuilder()

        override fun startInjecting(language: Language): MultiHostRegistrar {
            this.language = language
            return this
        }

        override fun doneInjecting() {
            if (started) registrar.doneInjecting()
        }

        override fun addPlace(prefix: String?, suffix: String?, host: PsiLanguageInjectionHost, rangeInsideHost: TextRange): MultiHostRegistrar {
            if (!started) {
                registrar.startInjecting(language!!)
                started = true
            }

            injected.append(prefix.orEmpty())
            injected.append(host.text.substring(rangeInsideHost.startOffset, rangeInsideHost.endOffset))
            injected.append(suffix.orEmpty())
            registrar.addPlace(prefix, suffix, host, rangeInsideHost)
            return this
        }

        fun addPlace(prefix: String? = null, suffix: String? = null, element: PsiElement): MultiHostRegistrar {
            return addPlace(prefix, suffix, conetxt, element.textRange)
        }

    }

    private fun appendSetDecls(prefixBuilder: StringBuilder, tokenDecls: List<CocoTokenDecl>, offset: Int = 1) {
        tokenDecls.forEachIndexed { index, cocoTokenDecl ->
            prefixBuilder.append("\tpublic static final int _${cocoTokenDecl.name} = ${index + offset};\n")
        }
    }
}