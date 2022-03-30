package no.fdk.fdk_reasoning_service.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestReport(
    val id: String,
    val url: String,
    val dataType: String,
    val harvestError: Boolean,
    val dateTime: String,
    val errorMessage: String?,
    val changedCatalogs: List<FdkIdAndUri>,
    val changedResources: List<FdkIdAndUri>
)

data class FdkIdAndUri(
    val fdkId: String,
    val uri: String
)
