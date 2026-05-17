package com.azuratech.azuratime

import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.runner.RunWith

@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(packages = ["com.azuratech.azuratime"])
class ArchitectureTest {

    @ArchTest
    val repositories_must_reside_in_data_layer: ArchRule = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .and().areNotInterfaces()
        .should().resideInAnyPackage("..data.repository..", "..data.repo..", "..data.repo..", "..repository..")
        .because("All repositories must be in the data layer.")
}
