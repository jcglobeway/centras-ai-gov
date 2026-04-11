/**
 * admin-api Spring Boot 애플리케이션 진입점.
 *
 * @EnableJpaRepositories와 @EntityScan으로 각 모듈의 adapter.outbound.persistence 패키지를 명시적으로 스캔한다.
 * 자동 스캔을 사용하지 않는 이유: 어댑터 등록을 RepositoryConfiguration에서 명시적으로 관리하기 때문이다.
 */
package com.publicplatform.ragops.adminapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(
    basePackages = [
        "com.publicplatform.ragops.identityaccess.adapter.outbound.persistence",
        "com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence",
        "com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence",
        "com.publicplatform.ragops.ingestionops.adapter.outbound.persistence",
        "com.publicplatform.ragops.qareview.adapter.outbound.persistence",
        "com.publicplatform.ragops.chatruntime.adapter.outbound.persistence",
        "com.publicplatform.ragops.documentregistry.adapter.outbound.persistence",
        "com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence",
        "com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence",
        "com.publicplatform.ragops.redteam.adapter.outbound.persistence",
    ],
)
@EntityScan(
    basePackages = [
        "com.publicplatform.ragops.identityaccess.adapter.outbound.persistence",
        "com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence",
        "com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence",
        "com.publicplatform.ragops.ingestionops.adapter.outbound.persistence",
        "com.publicplatform.ragops.qareview.adapter.outbound.persistence",
        "com.publicplatform.ragops.chatruntime.adapter.outbound.persistence",
        "com.publicplatform.ragops.documentregistry.adapter.outbound.persistence",
        "com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence",
        "com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence",
        "com.publicplatform.ragops.redteam.adapter.outbound.persistence",
    ],
)
class AdminApiApplication

fun main(args: Array<String>) {
    runApplication<AdminApiApplication>(*args)
}

