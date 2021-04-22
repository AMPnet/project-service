package com.ampnet.projectservice.persistence.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
class ProjectLocation(var lat: Double?, var long: Double?) : Serializable
