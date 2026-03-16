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
        "com.publicplatform.ragops.ingestionops.adapter.outbound.persistence",
        "com.publicplatform.ragops.qareview.adapter.outbound.persistence",
        "com.publicplatform.ragops.chatruntime.adapter.outbound.persistence",
        "com.publicplatform.ragops.documentregistry.adapter.outbound.persistence",
        "com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence",
    ],
)
@EntityScan(
    basePackages = [
        "com.publicplatform.ragops.identityaccess.adapter.outbound.persistence",
        "com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence",
        "com.publicplatform.ragops.ingestionops.adapter.outbound.persistence",
        "com.publicplatform.ragops.qareview.adapter.outbound.persistence",
        "com.publicplatform.ragops.chatruntime.adapter.outbound.persistence",
        "com.publicplatform.ragops.documentregistry.adapter.outbound.persistence",
        "com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence",
    ],
)
class AdminApiApplication

fun main(args: Array<String>) {
    runApplication<AdminApiApplication>(*args)
}

