# fdk-reasoning-service
A service that extends and enriches graphs of harvested resources, before other parts of FDK handles the data.

- Each resource-type is enriched in by the reasoner based on different [rules](https://github.com/Informasjonsforvaltning/fdk-reasoning-service/blob/main/src/main/kotlin/no/fdk/fdk_reasoning_service/service/Rules.kt)
- All organizations are extended with triples from [organization-catalog](https://organization-catalog.fellesdatakatalog.digdir.no/organizations)
- These code lists from FDK reference data is used to extend associated triples:
  - [LOS](https://data.norge.no/reference-data/los/themes-and-words) is extended for resource types: datasets, information models, services
  - [Eurovocs](https://data.norge.no/reference-data/eu/eurovocs) is extended for resource types: datasets, information models, services
  - [data themes](https://data.norge.no/reference-data/eu/data-themes) is extended for resource types: datasets, information models, services
  - [concept statuses](https://data.norge.no/reference-data/eu/concept-statuses) is extended for resource types: concepts
  - [concept subjects](https://data.norge.no/reference-data/digdir/concept-subjects) is extended for resource types: concepts
  - [IANA media types](https://data.norge.no/reference-data/iana/media-types) is extended for resource types: data services, datasets
  - [file types](https://data.norge.no/reference-data/eu/file-types) is extended for resource types: data services, datasets
  - [open licenses](https://data.norge.no/reference-data/open-licenses) is extended for resource types: datasets, information models
  - [linguistic systems](https://data.norge.no/reference-data/linguistic-systems) is extended for resource types: datasets, information models, services
  - [nations](https://data.norge.no/reference-data/geonorge/administrative-enheter/nasjoner) is extended for resource types: datasets, information models
  - [norwegian regions](https://data.norge.no/reference-data/geonorge/administrative-enheter/fylker) is extended for resource types: datasets, information models
  - [norwegian municipalities](https://data.norge.no/reference-data/geonorge/administrative-enheter/kommuner) is extended for resource types: datasets, information models
  - [access rights](https://data.norge.no/reference-data/eu/access-rights) is extended for resource types: datasets
  - [frequencies](https://data.norge.no/reference-data/eu/frequencies) is extended for resource types: datasets
  - [provenance](https://data.norge.no/reference-data/provenance-statements) is extended for resource types: datasets
  - [publisher types](https://data.norge.no/reference-data/adms/publisher-types) is extended for resource types: services
  - [adms statuses](https://data.norge.no/reference-data/adms/statuses) is extended for resource types: services
  - [role types](https://data.norge.no/reference-data/digdir/role-types) is extended for resource types: services
  - [evidence types](https://data.norge.no/reference-data/digdir/evidence-types) is extended for resource types: services
  - [channel types](https://data.norge.no/reference-data/digdir/service-channel-types) is extended for resource types: services
  - [main activities](https://data.norge.no/reference-data/eu/main-activities) is extended for resource types: services
  - [week days](https://data.norge.no/reference-data/schema/week-days) is extended for resource types: services

## Develop and run locally
### Requirements
- [maven](https://github.com/apache/maven) (recommended)
- java 17
- docker
- docker-compose

## Run tests
```
mvn verify
```

## Run locally
### docker-compose
```
docker-compose up -d rabbitmq
docker-compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=develop"
```

In another terminal:
```
docker-compose up -d
```
Alternatively run only one of the harvesters, e.g.
```
docker-compose up -d {relevant-harvester}
```
When reasoning is completed send request (log message contains `"Successfully sent reasoning completed message for {CATALOG-TYPE}"`)
```
curl http://localhost:8080/{catalog-type}
```

### Example - Datasets

```
docker-compose up -d rabbitmq
docker-compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=develop"
```

In a seperate terminal
```
docker-compose up -d dataset-harvester
```

Wait for successful reasoning, then run
```
curl http://localhost:8080/datasets
```
