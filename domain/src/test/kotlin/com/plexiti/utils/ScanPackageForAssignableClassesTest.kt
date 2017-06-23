package com.plexiti.utils

import org.assertj.core.api.Assertions
import org.junit.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class ScanPackageForAssignableClassesTest {

    class TestClass: ScanPackageForAssignableClassesTest()

    @Test
    fun testScan() {
        val classes = scanPackageForAssignableClasses(this::class.java.`package`.name, ScanPackageForAssignableClassesTest::class.java)
        Assertions.assertThat(classes).hasSize(2).contains(ScanPackageForAssignableClassesTest::class.java, TestClass::class.java)
    }

}
