package com.ampnet.projectservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataProtocolResolverConfig {

    @Bean
    fun dataProtocolResolver(): DataProtocolResolver {
        return DataProtocolResolver()
    }
}
