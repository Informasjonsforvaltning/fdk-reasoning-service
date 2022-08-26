package no.fdk.fdk_reasoning_service.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestReport(
    val id: String,
    val url: String,
    val changedCatalogs: List<FdkIdAndUri> = emptyList(),
    val changedResources: List<FdkIdAndUri> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown=true)
data class ReasoningReport(
    val id: String,
    val url: String,
    val dataType: String,
    val harvestError: Boolean,
    val startTime: String,
    val endTime: String,
    val errorMessage: String? = null,
    val changedCatalogs: List<FdkIdAndUri> = emptyList(),
    val changedResources: List<FdkIdAndUri> = emptyList()
)

data class FdkIdAndUri(
    val fdkId: String,
    val uri: String
)

data class RetryReportsWrap(
    val type: CatalogType,
    val retryCount: Int,
    val reports: List<HarvestReport>
)
