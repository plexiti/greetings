package com.plexiti.utils

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
fun scanPackageForAssignableClasses(packageName: String, assignableTo: Class<*>): List<Class<*>> {

    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(assignableTo))
    return scanner.findCandidateComponents(packageName)
        .map { Class.forName(it.beanClassName) }

}
