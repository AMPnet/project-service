package com.ampnet.projectservice.config

import com.ampnet.projectservice.grpc.walletservice.WalletService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("WalletMockConfig")
@Configuration
class WalletMockConfig {

    @Bean
    @Primary
    fun getWalletService(): WalletService {
        return Mockito.mock(WalletService::class.java)
    }
}
