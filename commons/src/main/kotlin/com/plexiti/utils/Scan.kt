package com.plexiti.utils

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.Flow
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import com.plexiti.commons.domain.Named
import com.plexiti.commons.domain.Value
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import javax.swing.text.html.HTML.Attribute.N
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

fun <N: Named> scanPackageForNamedClasses(packageName: String, assignableTo:  KClass<out N>): Map<Name, KClass<out N>> {
    @Suppress("unchecked_cast")
    return scanPackageForAssignableClasses(packageName, assignableTo)
        .map { it.java.newInstance() as Named }
        .associate { Pair(it.name, it::class) } as Map<Name, KClass<out N>>
}

fun <N: Named> scanPackageForClassNames(packageName: String, assignableTo: KClass<out N>): Map<KClass<out N>, Name> {
    return scanPackageForNamedClasses(packageName, assignableTo)
        .map { it.value.java.newInstance() }
        .associate { Pair( it::class, it.name) }
}
