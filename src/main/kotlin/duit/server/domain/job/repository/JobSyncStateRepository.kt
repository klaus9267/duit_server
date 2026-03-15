package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.entity.SourceType
import org.springframework.data.jpa.repository.JpaRepository

interface JobSyncStateRepository : JpaRepository<JobSyncState, SourceType>
