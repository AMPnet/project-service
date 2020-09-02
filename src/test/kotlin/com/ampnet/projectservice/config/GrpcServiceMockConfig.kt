package com.ampnet.projectservice.config

import com.ampnet.projectservice.grpc.mailservice.MailService
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.grpc.walletservice.WalletService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("GrpcServiceMockConfig")
@Configuration
class GrpcServiceMockConfig {

    @Bean
    @Primary
    fun getUserService(): UserService {
        return Mockito.mock(UserService::class.java)
    }

    @Bean
    @Primary
    fun getWalletService(): WalletService {
        return Mockito.mock(WalletService::class.java)
    }

    @Bean
    @Primary
    fun getMailService(): MailService {
        return Mockito.mock(MailService::class.java)
    }
}
