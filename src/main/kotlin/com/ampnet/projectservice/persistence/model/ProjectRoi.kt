package com.ampnet.projectservice.persistence.model

import javax.persistence.Embeddable

@Embeddable
class ProjectRoi(var from: Double, var to: Double)
