# FDK Reasoning Service

This application extends and enriches graphs of harvested resources, before other parts of FDK handles the data.

For a broader understanding of the system’s context, refer to
the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki. For more
specific context on this application, see the **Harvesting** subsystem section.

- Each resource-type is enriched in by the reasoner based on
  different [rules](https://github.com/Informasjonsforvaltning/fdk-reasoning-service/blob/main/src/main/kotlin/no/fdk/fdk_reasoning_service/service/Rules.kt)
- All organizations are extended with triples
  from [organization-catalog](https://organization-catalog.fellesdatakatalog.digdir.no/organizations)
- These code lists from FDK reference data is used to extend associated triples:
    - [LOS](https://data.norge.no/reference-data/los/themes-and-words) is extended for resource types: datasets,
      information models, services
    - [Eurovocs](https://data.norge.no/reference-data/eu/eurovocs) is extended for resource types: datasets, information
      models, services
    - [data themes](https://data.norge.no/reference-data/eu/data-themes) is extended for resource types: datasets,
      information models, services
    - [concept statuses](https://data.norge.no/reference-data/eu/concept-statuses) is extended for resource types:
      concepts
    - [concept subjects](https://data.norge.no/reference-data/digdir/concept-subjects) is extended for resource types:
      concepts
    - [IANA media types](https://data.norge.no/reference-data/iana/media-types) is extended for resource types: data
      services, datasets
    - [file types](https://data.norge.no/reference-data/eu/file-types) is extended for resource types: data services,
      datasets
    - [open licenses](https://data.norge.no/reference-data/open-licenses) is extended for resource types: datasets,
      information models
    - [linguistic systems](https://data.norge.no/reference-data/linguistic-systems) is extended for resource types:
      datasets, information models, services
    - [nations](https://data.norge.no/reference-data/geonorge/administrative-enheter/nasjoner) is extended for resource
      types: datasets, information models
    - [norwegian regions](https://data.norge.no/reference-data/geonorge/administrative-enheter/fylker) is extended for
      resource types: datasets, information models
    - [norwegian municipalities](https://data.norge.no/reference-data/geonorge/administrative-enheter/kommuner) is
      extended for resource types: datasets, information models
    - [access rights](https://data.norge.no/reference-data/eu/access-rights) is extended for resource types: datasets
    - [frequencies](https://data.norge.no/reference-data/eu/frequencies) is extended for resource types: datasets
    - [provenance](https://data.norge.no/reference-data/provenance-statements) is extended for resource types: datasets
    - [publisher types](https://data.norge.no/reference-data/adms/publisher-types) is extended for resource types:
      services
    - [adms statuses](https://data.norge.no/reference-data/adms/statuses) is extended for resource types: services
    - [role types](https://data.norge.no/reference-data/digdir/role-types) is extended for resource types: services
    - [evidence types](https://data.norge.no/reference-data/digdir/evidence-types) is extended for resource types:
      services
    - [channel types](https://data.norge.no/reference-data/digdir/service-channel-types) is extended for resource types:
      services
    - [main activities](https://data.norge.no/reference-data/eu/main-activities) is extended for resource types:
      services
    - [week days](https://data.norge.no/reference-data/schema/week-days) is extended for resource types: services

## Getting Started
These instructions will give you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Ensure you have the following installed:

- Java 21
- Maven
- Docker

### Running locally

#### Clone the repository

```sh
git clone https://github.com/Informasjonsforvaltning/fdk-reasoning-service.git
cd fdk-reasoning-service
```

#### Generate sources

Kafka messages are serialized using Avro. Avro schemas are located in ```kafka/schemas```. To generate sources from Avro
schema, run the following command:

```sh
mvn generate-sources    
```

#### Start Kafka cluster and setup topics/schemas

Topics and schemas are set up automatically when starting the Kafka cluster. Docker compose uses the scripts
```create-topics.sh``` and ```create-schemas.sh``` to set up topics and schemas.

```sh
docker-compose up -d
```

If you have problems starting kafka, check if all health checks are ok. Make sure number at the end (after 'grep')
matches desired topics.

#### Start application

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

#### Produce messages

Check if schema id is correct in the script. This should be 1 if there is only one schema in your registry.

```sh
sh ./kafka/produce-messages.sh
```

### Running tests

```sh
mvn verify
```
