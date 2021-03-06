package com.ampnet.projectservice.exception

enum class ErrorCode(val categoryCode: String, var specificCode: String, var message: String) {

    // Users: 03
    USER_ROLE_INVALID("03", "04", "Invalid user role"),

    // Organization: 06
    ORG_MISSING("06", "01", "Non existing organization"),
    ORG_DUPLICATE_USER("06", "04", "User is already a member of this organization"),
    ORG_DUPLICATE_INVITE("06", "05", "User is already invited"),
    ORG_DUPLICATE_NAME("06", "06", "Organization with this name already exists"),
    ORG_MEM_MISSING("06", "08", "Organization membership missing"),
    ORG_INVALID_INVITE("06", "09", "Invalid organization invitation"),

    // Project: 07
    PRJ_MISSING("07", "01", "Non existing project"),
    PRJ_DATE("07", "02", "Invalid date"),
    PRJ_MIN_ABOVE_MAX(
        "07", "08",
        "Min investment per user is higher than max investment per user"
    ),
    PRJ_MAX_FUNDS_TOO_HIGH("07", "09", "Expected funding is too high"),
    PRJ_MAX_FUNDS_PER_USER_TOO_HIGH("07", "10", "Max funding per user is too high"),
    PRJ_ROI("07", "11", "Invalid project ROI"),
    PRJ_WRITE_PRIVILEGE("07", "13", "Missing project write privilege"),

    // Internal: 08
    INT_FILE_STORAGE("08", "01", "Could not upload document on cloud file storage"),
    INT_GRPC_USER("08", "04", "Failed gRPC call to user service"),
    INT_DB("08", "07", "Database exception"),
    INT_REQUEST("08", "08", "Invalid controller request exception"),
    INT_GRPC_WALLET("08", "09", "Failed gRPC call to wallet service")
}
