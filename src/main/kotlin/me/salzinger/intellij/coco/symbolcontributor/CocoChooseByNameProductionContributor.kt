package me.salzinger.intellij.coco.symbolcontributor

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import me.salzinger.intellij.coco.filterByName
import me.salzinger.intellij.coco.findProductions

/**
 * Created by Thomas on 29/03/2015.
 */
class CocoChooseByNameProductionContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        return findProductions(project)
            .mapNotNull { it.name }
            .toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<NavigationItem> {
        return findProductions(project).filterByName(name).toTypedArray()
    }
}
