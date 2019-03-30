package at.scheinecker.intellij.coco.symbolcontributor

import at.scheinecker.intellij.coco.CocoUtil
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project

/**
 * Created by Thomas on 29/03/2015.
 */
class CocoChooseByNameTokenContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        return CocoUtil.findTokenDecls(project)
                .mapNotNull { it.name }
                .toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        return CocoUtil.findTokenDecls(project, name).toTypedArray()
    }
}