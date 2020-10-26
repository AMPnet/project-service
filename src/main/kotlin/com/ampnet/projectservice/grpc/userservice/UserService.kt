package com.ampnet.projectservice.grpc.userservice

import com.ampnet.userservice.proto.UserResponse
import java.util.UUID

interface UserService {
    fun getUsers(uuids: Iterable<UUID>): List<UserResponse>
    fun getUsersByEmail(coop: String, emails: List<String>): List<UserResponse>
}
