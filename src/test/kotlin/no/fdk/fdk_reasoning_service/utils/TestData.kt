package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.service.UNION_ID
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

val allDatasetIds = listOf(
    UNION_ID,
    "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f",
    "4667277a-9d27-32c1-aed5-612fa601f393",
    "a1c680ca-62d7-34d5-aa4c-d39b5db033ae",
    "0801ece7-8e74-385f-9db4-44e5c6508c44",
    "bc56fa1c-434a-3561-8fa7-e373dc1b6b55"
)
const val DATASET_CATALOG_ID = "6e4237cc-98d6-3e7c-a892-8ac1f0ffb37f"
const val DATASET_ID_0 = "4667277a-9d27-32c1-aed5-612fa601f393"

val savedDatasetCollections = listOf(
    "fdkCatalogs",
    "fdkCatalogs",
    "fdkDatasets",
    "fdkDatasets",
    "fdkDatasets",
    "fdkDatasets")

val allEventIds = listOf(
    UNION_ID,
    "12af419b-b010-3acd-bd4d-c52c3a62990f",
    "d546ae30-8bb6-3f29-9e38-de7a1c4a0278",
    "d0181a04-f2ee-3d86-9298-ce574b2736f0",
    "bc56fa1c-434a-3561-8fa7-e373dc1b6b55",
    "fa7176b4-7743-3543-8c71-86c46e7f3654",
    "92bf30ca-9fc1-35c7-ac7e-88b0188a2dc9",
    "42396ae0-fb4a-3662-85b0-301d2e0a8189",
    "2fda2afd-9087-337b-8b16-23627ccaa9c4",
    "3b0a716a-7ce4-3c1f-9b74-071e737893f8",
    "f0fea636-c6f9-3868-9b4c-5769d142d2b8"
)
const val EVENT_ID_0 = "2fda2afd-9087-337b-8b16-23627ccaa9c4"

val allDataServiceIds = listOf(
    UNION_ID,
    "e422e2a7-287f-349f-876a-dc3541676f21",
    "ea51178e-f843-3025-98c5-7d02ce887f90",
    "4667277a-9d27-32c1-aed5-612fa601f393"
)
const val DATA_SERVICE_CATALOG_ID = "e422e2a7-287f-349f-876a-dc3541676f21"
const val DATA_SERVICE_ID_0 = "ea51178e-f843-3025-98c5-7d02ce887f90"

val allConceptIds = listOf(
    UNION_ID,
    "9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    "7dbac738-4944-323a-a777-ad2f83bf75f8",
    "db1b701c-b4b9-3c20-bc23-236a91236754"
)
const val CONCEPT_COLLECTION_ID = "9b8f1c42-1161-33b1-9d43-a733ee94ddfc"
const val CONCEPT_ID_0 = "db1b701c-b4b9-3c20-bc23-236a91236754"

val allPublicServiceIds = listOf(
    UNION_ID,
    "d5d0c07c-c14f-3741-9aa3-126960958cf0", "6ce4e524-3226-3591-ad99-c026705d4259",
    "31249174-df02-3746-9d61-59fc61b4c5f9", "da906206-b621-306c-bd75-f2bfe5cc9b4f",
    "a8ad6252-55a6-3ea8-b714-c80c115f5d07", "bb537e5b-11ca-3bca-bbe5-2f8763543740",
    "209c12a4-0b55-3dfc-9905-576d56b58f85", "0801ece7-8e74-385f-9db4-44e5c6508c44",
    "0ac9dc02-7ec0-38b8-9345-19b304127c3b", "62966d9d-a547-3a66-a588-3fdf0f97d885",
    "7e3805d3-c187-3feb-9cc8-7015eb357ac7", "7a04cd11-13cb-3dc2-9e31-822e9135ad08",
    "c9a00b57-f1b1-3718-a9aa-0837dd297eec", "88dac895-0c38-30d0-97d3-404181dd17cc",
    "ccec51fe-a499-32b4-8c08-62c1676692d2", "536bfa22-0126-3032-b31a-af8d05b7b0ae",
    "0f0ef27a-9a7e-341e-8ee6-e5a6cf09e7a4", "487787c8-03c4-3b9d-9382-710f21219d91")
const val PUBLIC_SERVICE_ID_0 = "62966d9d-a547-3a66-a588-3fdf0f97d885"

const val INFOMODEL_CATALOG_ID = "f25c939d-0722-3aa3-82b1-eaa457086444"
const val INFOMODEL_0_ID = "bcbe6738-85f6-388c-afcc-ef1fafd7cc45"
