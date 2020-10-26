package com.ampnet.projectservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.projectservice")
class ApplicationProperties {
    var jwt: JwtProperties = JwtProperties()
    var fileStorage: FileStorageProperties = FileStorageProperties()
    var investment: InvestmentProperties = InvestmentProperties()
    var grpc: GrpcProperties = GrpcProperties()
    val coop: CoopProperties = CoopProperties()
}

class JwtProperties {
    lateinit var publicKey: String
}

class FileStorageProperties {
    lateinit var url: String
    lateinit var bucket: String
    lateinit var folder: String
}

@Suppress("MagicNumber")
class InvestmentProperties {
    var maxPerProject: Long = 100_000_000_000_000_00
    var maxPerUser: Long = 1_000_000_000_000_00
}

@Suppress("MagicNumber")
class GrpcProperties {
    var mailServiceTimeout: Long = 1000
    var userServiceTimeout: Long = 1000
    var walletServiceTimeout: Long = 1000
}

class CoopProperties {
    lateinit var default: String
}
