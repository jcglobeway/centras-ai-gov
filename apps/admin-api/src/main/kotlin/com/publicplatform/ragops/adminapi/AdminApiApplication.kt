package com.publicplatform.ragops.adminapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = [
        "com.publicplatform.ragops.identityaccess",
        "com.publicplatform.ragops.organizationdirectory",
        "com.publicplatform.ragops.ingestionops",
    ],
)
@EntityScan(
    basePackages = [
        "com.publicplatform.ragops.identityaccess",
        "com.publicplatform.ragops.organizationdirectory",
        "com.publicplatform.ragops.ingestionops",
    ],
)
class AdminApiApplication

fun main(args: Array<String>) {
    runApplication<AdminApiApplication>(*args)
}

