package com.ampnet.projectservice.persistence.model

import javax.persistence.Embeddable

@Embeddable
data class ProjectLocation(var lat: Double, var long: Double)
