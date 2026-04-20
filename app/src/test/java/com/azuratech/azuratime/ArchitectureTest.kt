package com.azuratech.azuratime

import org.junit.runner.RunWith
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue

@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(packages = ["com.azuratech.azuratime"])
class ArchitectureTest {

    @ArchTest
    val layer_dependencies_are_respected: ArchRule = layeredArchitecture()
        .consideringAllDependencies()
        .layer("UI").definedBy("com.azuratech.azuratime.ui..", "com.azuratech.azuratime")
        .layer("Domain").definedBy("com.azuratech.azuratime.domain..")
        .layer("Data").definedBy("com.azuratech.azuratime.data..", "com.azuratech.azuratime.repository..")
        .layer("Core").definedBy("com.azuratech.azuratime.core..", "com.azuratech.azuratime.utils..")
        .layer("DI").definedBy("com.azuratech.azuratime.di..", "com.azuratech.azuratime.core.di..")
        .layer("ML").definedBy("com.azuratech.azuratime.ml..")
        .whereLayer("UI").mayOnlyBeAccessedByLayers("DI", "Core", "Domain", "Data")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("UI", "Data", "Core", "DI", "ML")
        .whereLayer("Data").mayOnlyBeAccessedByLayers("Domain", "UI", "Core", "DI", "ML")
        .whereLayer("Core").mayOnlyBeAccessedByLayers("UI", "Data", "Domain", "DI", "ML")
        .whereLayer("ML").mayOnlyBeAccessedByLayers("UI", "Domain", "Data", "DI", "Core")
        .whereLayer("DI").mayNotBeAccessedByAnyLayer()
        .ignoreDependency(alwaysTrue(), resideInAPackage("com.azuratech.azuratime.R.."))
        .ignoreDependency(alwaysTrue(), simpleNameContaining("R\$"))
        .ignoreDependency(alwaysTrue(), simpleName("R"))
        .ignoreDependency(simpleNameContaining("Hilt"), alwaysTrue())
        .ignoreDependency(simpleNameContaining("Dagger"), alwaysTrue())
        .ignoreDependency(resideInAnyPackage("com.azuratech.azuratime"), alwaysTrue())
        .ignoreDependency(alwaysTrue(), resideInAnyPackage("com.azuratech.azuratime"))

    @ArchTest
    val repositories_must_reside_in_data_layer: ArchRule = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .and().areNotInterfaces()
        .should().resideInAnyPackage("..data.repository..", "..data.repo..", "..data.repo..", "..repository..")
        .because("All repositories must be in the data layer.")
}
