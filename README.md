# Project Service

[![CircleCI](https://circleci.com/gh/AMPnet/project-service/tree/master.svg?style=svg&circle-token=314ec3a03b6c8111c15e7fde04a01f6d387f28bc)](https://circleci.com/gh/AMPnet/project-service/tree/master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/aae9cf1e57cc4f9ba2aae440c23f2832)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AMPnet/project-service&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/AMPnet/project-service/branch/master/graph/badge.svg)](https://codecov.io/gh/AMPnet/project-service)

Project service is a part of the AMPnet crowdfunding project. Service contains organization and project data. 
Using gRPC, service is connected to other crowdfunding services:
  * [user service](https://github.com/AMPnet/user-service)
  * [mail service](https://github.com/AMPnet/mail-service)

## Requirements

Service must have running and initialized database. Default database url is `locahost:5432`.
To change database url set configuration: `spring.datasource.url` in file `application.properties`.
To initialize the database run script in the project root folder:

```sh
./initialize-local-database.sh
```

## Start

Application is running on port: `8123`. To change default port set configuration: `server.port`.

### Build

```sh
./gradlew build
```

### Run

```sh
./gradlew bootRun
```

After starting the application, API documentation is available at: `localhost:8123/docs/index.html`.
If documentation is missing generate it by running gradle task:
```sh
./gradlew copyDocs
```

### Test

```sh
./gradlew test
```
