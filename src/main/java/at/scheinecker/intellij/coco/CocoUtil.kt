package at.scheinecker.intellij.coco

import at.scheinecker.intellij.coco.action.CocoRAction
import at.scheinecker.intellij.coco.psi.*
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.notification.Notifications
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import org.apache.commons.lang.StringUtils
import org.jetbrains.annotations.Contract
import java.util.*

/**
 * @author Thomas Scheinecker [tscheinecker@gmail.com](mailto:tscheinecker@gmail.com)
 */
object CocoUtil {

    fun findCompilerNames(project: Project?): List<String> {
        return findCompilers(project)
                .map { it.name }
                .filterNotNull()
    }

    @Contract("_, null -> null")
    fun findCompiler(file: PsiFile, name: String?): CocoCompiler? {
        return findByName(findCompilers(file), name)
    }

    fun findCompilers(file: PsiFile): List<CocoCompiler> {
        return PsiTreeUtil.findChildrenOfType(file, CocoCompiler::class.java).toList();
    }

    fun findCompilers(project: Project?, name: String): List<CocoCompiler> {
        return findCompilers(project)
                .filter { compiler -> name == compiler.name }
    }

    fun findCompilers(project: Project?): List<CocoCompiler> {
        if (project == null) {
            return emptyList()
        }

        return FileBasedIndex
                .getInstance()
                .getContainingFiles<FileType, Void>(FileTypeIndex.NAME, CocoFileType.INSTANCE, GlobalSearchScope.allScope(project))
                .mapNotNull { PsiManager.getInstance(project).findFile(it) as CocoFile? }
                .flatMap { PsiTreeUtil.getChildrenOfType(it, CocoCompiler::class.java)?.asList() ?: emptyList() }
    }

    fun findCharacterDeclarations(file: PsiFile): List<CocoSetDecl> {
        val scannerSpecification = PsiTreeUtil.getChildOfType(file, CocoScannerSpecification::class.java) ?: return emptyList()

        val characters = scannerSpecification.characters ?: return emptyList()

        return characters.setDeclList
    }

    @Contract("_, null -> null")
    fun findCharacterDeclaration(file: PsiFile, name: String?): CocoSetDecl? {
        return findByName(findCharacterDeclarations(file), name)
    }

    fun getDeclaredPackage(file: PsiFile): String? {
        return PsiTreeUtil.getChildOfType(file, CocoPackageDirective::class.java)?.declaredPackage?.text?.trim();
    }

    fun getTargetPackage(file: PsiFile): Optional<String> {
        val declaredPackage = getDeclaredPackage(file)
        if (declaredPackage != null) {
            return Optional.of(declaredPackage)
        }

        return Optional.ofNullable(JavaDirectoryService.getInstance().getPackage(file.containingDirectory!!)?.qualifiedName)
    }

    fun getParserClass(file: PsiFile): PsiClass? {
        val javaPsiFacade = ServiceManager.getService(file.project, JavaPsiFacade::class.java)

        val parserClassName = "${getTargetPackage(file).map { "${it}." }.orElse("")}Parser"
        return javaPsiFacade.findClass(parserClassName, GlobalSearchScope.allScope(javaPsiFacade.project))
    }

    fun getJavaInfos(file: PsiClass) {

        val instance = CodeSmellDetector.getInstance(file.project)
        instance.findCodeSmells(listOf(file.containingFile.virtualFile))
                .filter { it.severity == HighlightSeverity.ERROR }
                .forEach {
                    Notifications.Bus.notify(
                            CocoRAction.COCO_NOTIFICATION_GROUP.createNotification(
                                    "${it.description} [${it.startLine}:${it.startColumn}]",
                                    MessageType.ERROR
                            )
                    )
                }
    }

    fun findProductions(file: PsiFile): List<CocoProduction> {
        val parserSpecification = PsiTreeUtil.getChildOfType(file, CocoParserSpecification::class.java) ?: return emptyList()

        return parserSpecification.productionList
    }

    @Contract("_, null -> null")
    fun findProduction(file: PsiFile, name: String?): CocoProduction? {
        return findByName(findProductions(file), name)
    }

    fun findTokenDecls(file: PsiFile): List<CocoTokenDecl> {
        val scannerSpecification = PsiTreeUtil.getChildOfType(file, CocoScannerSpecification::class.java) ?: return emptyList()

        val tokens = scannerSpecification.tokens ?: return emptyList()

        return tokens.tokenDeclList
    }

    fun findPragmaDecls(file: PsiFile): List<CocoPragmaDecl> {
        val scannerSpecification = PsiTreeUtil.getChildOfType(file, CocoScannerSpecification::class.java) ?: return emptyList()

        val pragmas = scannerSpecification.pragmas ?: return emptyList()

        return pragmas.pragmaDeclList
    }

    fun findNearestCocoNamedElement(element: PsiElement): CocoNamedElement {
        var searchElement: PsiElement? = element
        while (searchElement != null) {
            if (searchElement is CocoNamedElement) {
                return searchElement
            }
            searchElement = searchElement.parent
        }

        throw NullPointerException("Couldn't find CocoNamedElement for element $element")
    }

    fun findProductions(project: Project?): List<CocoProduction> {
        val characterDecls = ArrayList<CocoProduction>()
        val allFiles = getAllFiles(project)
        for (file in allFiles) {
            characterDecls.addAll(findProductions(file))
        }
        return characterDecls
    }

    fun findCharacterDecls(project: Project?): List<CocoSetDecl> {
        val characterDecls = ArrayList<CocoSetDecl>()
        val allFiles = getAllFiles(project)
        for (file in allFiles) {
            characterDecls.addAll(findCharacterDeclarations(file))
        }
        return characterDecls
    }

    fun findTokenDecls(project: Project?): List<CocoTokenDecl> {
        val tokenDecls = ArrayList<CocoTokenDecl>()
        val allFiles = getAllFiles(project)
        for (file in allFiles) {
            tokenDecls.addAll(findTokenDecls(file))
        }
        return tokenDecls
    }

    private fun getAllFiles(project: Project?): List<PsiFile> {
        if (project == null) {
            return emptyList()
        }

        val virtualFiles = FileBasedIndex.getInstance().getContainingFiles<FileType, Void>(FileTypeIndex.NAME, CocoFileType.INSTANCE, GlobalSearchScope.allScope(project))

        val psiFiles = ArrayList<PsiFile>()
        val psiManager = PsiManager.getInstance(project)

        for (virtualFile in virtualFiles) {
            val file = psiManager.findFile(virtualFile)
            if (file != null) {
                psiFiles.add(file)
            }
        }

        return psiFiles
    }

    fun findTokenDecls(project: Project?, name: String?): List<CocoTokenDecl> {
        return findAllByName(findTokenDecls(project), name)
    }

    fun findCharacterDecls(project: Project?, name: String?): List<CocoSetDecl> {
        return findAllByName(findCharacterDecls(project), name)
    }

    fun findProductions(project: Project?, name: String?): List<CocoProduction> {
        return findAllByName(findProductions(project), name)
    }

    @Contract("_, null -> null")
    fun findTokenDecl(file: PsiFile, name: String?): CocoTokenDecl? {
        return findByName(findTokenDecls(file), name)
    }

    @Contract("_, null -> null")
    private fun <T : CocoNamedElement> findByName(collection: List<T>, name: String?): T? {
        if (StringUtils.isBlank(name)) {
            return null
        }

        return findAllByName(collection, name).firstOrNull()
    }

    private fun <T : PsiNameIdentifierOwner> findAllByName(collection: List<T>, name: String?): List<T> {
        return if (StringUtils.isBlank(name)) {
            collection
        } else collection
                .filter { item -> name == item.name }

    }

    fun findGlobalFieldsAndMethods(file: PsiFile): CocoGlobalFieldsAndMethods? {
        return PsiTreeUtil.getChildOfType(file, CocoGlobalFieldsAndMethods::class.java)
    }
}
