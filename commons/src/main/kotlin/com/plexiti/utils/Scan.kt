package com.plexiti.utils

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
fun scanPackageForAssignableClasses(packageName: String, assignableTo: Class<*>): List<Class<*>> {

    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(assignableTo))
    return scanner.findCandidateComponents(packageName)
        .filter { it.beanClassName != assignableTo.name }
        .map { Class.forName(it.beanClassName) }

}

fun scanPackageForAssignableClasses(packageName: String, assignableTo: KClass<*>): List<KClass<*>> {

    return scanPackageForAssignableClasses(packageName, assignableTo.java).map { it.kotlin }

}
