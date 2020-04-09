package com.ampnet.projectservice.persistence.model

import javax.persistence.Embeddable

@Embeddable
data class ProjectRoi(var from: Double, var to: Double)
