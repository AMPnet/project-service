package com.ampnet.projectservice.grpc

import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.proto.GetByUuids
import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.OrganizationsResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.projectservice.proto.ProjectServiceGrpc
import com.ampnet.projectservice.proto.ProjectsResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcProjectServer(
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository
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
            .mapNotNull { organizationToGprcResponse(it) }
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

    private fun organizationToGprcResponse(organization: Organization): OrganizationResponse {
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

    private fun projectToGrpcResponse(project: Project): ProjectResponse {
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
            .setDescription(project.description)
            .setCoop(project.coop)
        project.mainImage?.let { builder.setImageUrl(it) }
        return builder.build()
    }
}
