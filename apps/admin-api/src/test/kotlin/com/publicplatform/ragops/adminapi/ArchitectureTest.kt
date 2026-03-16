package com.publicplatform.ragops.adminapi

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

@AnalyzeClasses(
    packages = ["com.publicplatform.ragops"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {

    // Rule 1: domain 패키지 클래스는 JPA/Spring Data 접근 금지
    @ArchTest
    val domain_must_not_depend_on_jpa: ArchRule =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().accessClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
            .because("domain 레이어는 프레임워크에 의존하면 안 됩니다")

    // Rule 2: application.port.out 패키지 클래스는 adapter.outbound.persistence에 의존 금지 (역방향)
    @ArchTest
    val ports_must_not_depend_on_adapters: ArchRule =
        noClasses()
            .that().resideInAPackage("..application.port.out..")
            .should().accessClassesThat()
            .resideInAPackage("..adapter.outbound.persistence..")
            .because("port 인터페이스는 adapter 구현체에 의존하면 안 됩니다")

    // Rule 3: application.port.in 패키지 클래스는 adapter.outbound.persistence에 의존 금지
    @ArchTest
    val inbound_ports_must_not_depend_on_adapters: ArchRule =
        noClasses()
            .that().resideInAPackage("..application.port.in..")
            .should().accessClassesThat()
            .resideInAPackage("..adapter.outbound.persistence..")
            .because("inbound port 인터페이스는 adapter 구현체에 의존하면 안 됩니다")

    // Rule 4: Controller는 adapter.outbound.persistence를 직접 참조 금지
    @ArchTest
    val controllers_must_not_access_persistence: ArchRule =
        noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().accessClassesThat()
            .resideInAPackage("..adapter.outbound.persistence..")
            .because("Controller는 UseCase 인터페이스를 통해서만 데이터에 접근해야 합니다")

    // Rule 5: Jpa*Repository는 adapter.outbound.persistence 패키지 또는 RepositoryConfiguration에서만 접근
    @ArchTest
    val jpa_repos_only_accessed_by_adapters: ArchRule =
        classes()
            .that().haveSimpleNameStartingWith("Jpa")
            .and().haveSimpleNameEndingWith("Repository")
            .should().onlyBeAccessed().byClassesThat(
                object : DescribedPredicate<JavaClass>("residing in adapter.outbound.persistence or named RepositoryConfiguration") {
                    override fun test(t: JavaClass) =
                        t.packageName.contains("adapter.outbound.persistence") || t.simpleName == "RepositoryConfiguration"
                }
            )
            .because("JPA Repository는 Adapter를 통해서만 접근해야 합니다")

    // Rule 6: 모듈 간 순환 의존성 금지
    @ArchTest
    val no_cycles: ArchRule =
        slices()
            .matching("com.publicplatform.ragops.(*)..")
            .should().beFreeOfCycles()
}
