package com.ampnet.projectservice.grpc

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.proto.GetByUuid
import com.ampnet.projectservice.proto.GetByUuids
import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.projectservice.proto.OrganizationMembershipsResponse
import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.OrganizationsResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.projectservice.proto.ProjectServiceGrpc
import com.ampnet.projectservice.proto.ProjectWithDataResponse
import com.ampnet.projectservice.proto.ProjectsResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcProjectServer(
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository
) : ProjectServiceGrpc.ProjectServiceImplBase() {

    companion object : KLogging()

    override fun getOrganizations(request: GetByUuids, responseObserver: StreamObserver<OrganizationsResponse>) {
        logger.debug { "Received gRPC request GetOrganizations: ${request.uuidsList}" }
        val uuids = request.uuidsList.mapNotNull {
            try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                logger.warn(ex.message)
                null
            }
        }
        val organizationResponses = organizationRepository.findAllById(uuids)
            .mapNotNull { organizationToGrpcResponse(it) }
        val response = OrganizationsResponse.newBuilder()
            .addAllOrganizations(organizationResponses)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getProjects(request: GetByUuids, responseObserver: StreamObserver<ProjectsResponse>) {
        logger.debug { "Received gRPC request GetProjects: ${request.uuidsList}" }
        val uuids = request.uuidsList.mapNotNull {
            try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                logger.warn(ex.message)
                null
            }
        }
        val projectResponses = projectRepository.findAllById(uuids)
            .mapNotNull { projectToGrpcResponse(it) }
        val response = ProjectsResponse.newBuilder()
            .addAllProjects(projectResponses)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getOrganizationMembersForProject(
        request: GetByUuid,
        responseObserver: StreamObserver<OrganizationMembershipsResponse>
    ) {
        logger.debug { "Received gRPC request getOrganizationMembers for project: ${request.projectUuid}" }
        val organizationMembers =
            organizationMembershipRepository.findByProjectUuid(UUID.fromString(request.projectUuid))
        val response = OrganizationMembershipsResponse.newBuilder()
            .addAllMemberships(organizationMembers.map { membershipToGrpcResponse(it) }).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getProjectWithData(request: GetByUuid, responseObserver: StreamObserver<ProjectWithDataResponse>) {
        logger.debug { "Received gRPC request getProjectFull: ${request.projectUuid}" }
        projectRepository.findByIdWithAllData(UUID.fromString(request.projectUuid)).ifPresent { project ->
            val builder = ProjectWithDataResponse.newBuilder()
                .setProject(projectToGrpcResponse(project))
            project.termsOfService?.let { builder.setTosUrl(it.link) }
            val response = builder.build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
            return@ifPresent
        }
        val exception = ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: ${request.projectUuid}")
        logger.warn { exception.message }
        responseObserver.onError(exception)
    }

    private fun organizationToGrpcResponse(organization: Organization): OrganizationResponse {
        val builder = OrganizationResponse.newBuilder()
            .setUuid(organization.uuid.toString())
            .setName(organization.name)
            .setCreatedByUser(organization.createdByUserUuid.toString())
            .setCreatedAt(organization.createdAt.toInstant().toEpochMilli())
            .setApproved(organization.approved)
            .setCoop(organization.coop)
        organization.description?.let { builder.setDescription(it) }
        organization.headerImage?.let { builder.setHeaderImage(it) }
        return builder.build()
    }

    internal fun projectToGrpcResponse(project: Project): ProjectResponse {
        val builder = ProjectResponse.newBuilder()
            .setUuid(project.uuid.toString())
            .setName(project.name)
            .setCreatedByUser(project.createdByUserUuid.toString())
            .setStartDate(project.startDate.toInstant().toEpochMilli())
            .setEndDate(project.endDate.toInstant().toEpochMilli())
            .setMinPerUser(project.minPerUser)
            .setMaxPerUser(project.maxPerUser)
            .setExpectedFunding(project.expectedFunding)
            .setCurrency(project.currency.name)
            .setActive(project.active)
            .setOrganizationUuid(project.organization.uuid.toString())
            .setDescription(project.shortDescription.orEmpty())
            .setCoop(project.coop)
        project.mainImage?.let { builder.setImageUrl(it) }
        return builder.build()
    }

    private fun membershipToGrpcResponse(membership: OrganizationMembership): OrganizationMembershipResponse =
        OrganizationMembershipResponse.newBuilder()
            .setUserUuid(membership.userUuid.toString())
            .setOrganizationUuid(membership.organizationUuid.toString())
            .setRole(getOrganizationRole(membership.role))
            .setMemberSince(membership.createdAt.toInstant().toEpochMilli())
            .build()

    private fun getOrganizationRole(type: OrganizationRole): OrganizationMembershipResponse.Role =
        when (type) {
            OrganizationRole.ORG_ADMIN -> OrganizationMembershipResponse.Role.ORG_ADMIN
            OrganizationRole.ORG_MEMBER -> OrganizationMembershipResponse.Role.ORG_MEMBER
        }
}
