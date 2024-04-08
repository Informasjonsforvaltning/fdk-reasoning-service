package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.model.FdkIdAndUri
import no.fdk.fdk_reasoning_service.model.HarvestReport
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.time.Instant
import java.util.Date

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

val allDatasetIds = listOf(
    "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
    "4667277a-9d27-32c1-aed5-612fa601f393",
    "a1c680ca-62d7-34d5-aa4c-d39b5db033ae",
    "0801ece7-8e74-385f-9db4-44e5c6508c44",
    "bc56fa1c-434a-3561-8fa7-e373dc1b6b55"
)
const val DATASET_CATALOG_ID = "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f"
const val DATASET_ID_0 = "4667277a-9d27-32c1-aed5-612fa601f393"
val DATASET_REPORT = HarvestReport(id = "id", "https://datasets.com",
    changedCatalogs = listOf(FdkIdAndUri(DATASET_CATALOG_ID, "https://datasets.com/$DATASET_CATALOG_ID")))

val savedDatasetCollections = listOf(
    "fdkCatalogs",
    "fdkDatasets",
    "fdkDatasets",
    "fdkDatasets",
    "fdkDatasets"
)

const val EVENT_ID_0 = "2fda2afd-9087-337b-8b16-23627ccaa9c4"
const val EVENT_ID_1 = "12af419b-b010-3acd-bd4d-c52c3a62990f"
const val EVENT_CATALOG_ID = "4d2c9e29-2f9a-304f-9e48-34e30a36d068"
val EVENT_REPORT = HarvestReport(id = "id", "https://events.com",
    changedCatalogs = listOf(FdkIdAndUri(EVENT_CATALOG_ID, "http://localhost:5050/events/catalogs/$EVENT_CATALOG_ID")),
    changedResources = listOf(
        FdkIdAndUri(EVENT_ID_0, "https://events.com/$EVENT_ID_0"),
        FdkIdAndUri(EVENT_ID_1, "https://events.com/$EVENT_ID_1")))

val allDataServiceIds = listOf(
    "e422e2a7-287f-349f-876a-dc3541676f21",
    "ea51178e-f843-3025-98c5-7d02ce887f90",
    "4667277a-9d27-32c1-aed5-612fa601f393"
)
const val DATA_SERVICE_CATALOG_ID = "e422e2a7-287f-349f-876a-dc3541676f21"
const val DATA_SERVICE_ID_0 = "ea51178e-f843-3025-98c5-7d02ce887f90"
val DATA_SERVICE_REPORT = HarvestReport(id = "id", "https://data-services.com",
    changedCatalogs = listOf(FdkIdAndUri(DATA_SERVICE_CATALOG_ID, "https://data-services.com/$DATA_SERVICE_CATALOG_ID")))

val allConceptIds = listOf(
    "9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    "7dbac738-4944-323a-a777-ad2f83bf75f8",
    "db1b701c-b4b9-3c20-bc23-236a91236754"
)
const val CONCEPT_COLLECTION_ID = "9b8f1c42-1161-33b1-9d43-a733ee94ddfc"
const val CONCEPT_ID_0 = "db1b701c-b4b9-3c20-bc23-236a91236754"
val CONCEPT_REPORT = HarvestReport(id = "id", "https://concepts.com",
    changedCatalogs = listOf(FdkIdAndUri(CONCEPT_COLLECTION_ID, "https://concepts.com/$CONCEPT_COLLECTION_ID")))

const val PUBLIC_SERVICE_ID_0 = "62966d9d-a547-3a66-a588-3fdf0f97d885"
const val PUBLIC_SERVICE_ID_1 = "d5d0c07c-c14f-3741-9aa3-126960958cf0"
const val PUBLIC_SERVICE_CATALOG_0_ID = "4d2c9e29-2f9a-304f-9e48-34e30a36d068"
const val PUBLIC_SERVICE_CATALOG_1_ID = "8d2c9e29-2f9a-304f-9e48-34e30a36d068"
val PUBLIC_SERVICE_REPORT = HarvestReport(id = "id", "https://public-services.com",
    changedCatalogs = listOf(
        FdkIdAndUri(PUBLIC_SERVICE_CATALOG_0_ID, "http://localhost:5050/public-services/catalogs/$PUBLIC_SERVICE_CATALOG_0_ID"),
        FdkIdAndUri(PUBLIC_SERVICE_CATALOG_1_ID, "http://localhost:5050/public-services/catalogs/$PUBLIC_SERVICE_CATALOG_1_ID")),
    changedResources = listOf(
        FdkIdAndUri(PUBLIC_SERVICE_ID_0, "https://public-services.com/$PUBLIC_SERVICE_ID_0"),
        FdkIdAndUri(PUBLIC_SERVICE_ID_1, "https://public-services.com/$PUBLIC_SERVICE_ID_1")))
val PUBLIC_SERVICE_REPORT_0 = HarvestReport(id = "id", "https://public-services.com",
    changedCatalogs = listOf(FdkIdAndUri(PUBLIC_SERVICE_CATALOG_0_ID, "http://localhost:5050/public-services/catalogs/$PUBLIC_SERVICE_CATALOG_0_ID")),
    changedResources = listOf(FdkIdAndUri(PUBLIC_SERVICE_ID_0, "https://public-services.com/$PUBLIC_SERVICE_ID_0")))

const val INFOMODEL_CATALOG_ID = "f25c939d-0722-3aa3-82b1-eaa457086444"
const val INFOMODEL_0_ID = "bcbe6738-85f6-388c-afcc-ef1fafd7cc45"
val INFOMODEL_REPORT = HarvestReport(id = "id", "https://information-models.com",
    changedCatalogs = listOf(FdkIdAndUri(INFOMODEL_CATALOG_ID, "https://information-models.com/$INFOMODEL_CATALOG_ID")))


val TEST_DATE: Date = Date.from(Instant.ofEpochSecond(1651729181))
