# veo-forms
Spring boot micro service for veo forms.

### Build dependencies
* Docker

By default, integration tests use testcontainers to launch an embedded PostgreSQL DB. This requires docker.
If you wish to use your [local PostgreSQL DB](#create-postgresql-db) instead, apply the `spring.datasource.[...]` config
from [application.properties](/src/main/resources/application.properties) to your
[application_test.properties](/src/test/resources/application-test.properties).

## Build

    ./gradlew build

For verification, I recommend this as a `pre-commit` git hook.

## Runtime dependencies
* PostgreSQL DB
* OAuth server

## Config & Launch
### Create PostgreSQL DB
Install postgres and create veo-forms database:

    su postgres
    createuser -S -D -R -P verinice
    # when prompted set password to "verinice"
    createdb -O verinice veo-forms
    exit

You can customize connection settings in `application.properties` > `spring.datasource.[...]`.

### Configure OAuth
Setup OAuth server URLs (`application.properties` > `spring.security.oauth2.resourceserver.jwt.[...]`).

### Run

    ./gradlew bootRun

(default port: 8080)


## API docs
Launch and visit <http://localhost:8080/swagger-ui.html>


## Code format
Spotless is used for linting and license-gradle-plugin is used to apply license headers. The following task applies
spotless code format & adds missing license headers to new files:

    ./gradlew formatApply

The Kotlin lint configuration does not allow wildcard imports. Spotless cannot fix wildcard imports automatically, so
you should setup your IDE to avoid them.

## Database migrations
Veo-forms uses [flyway](https://github.com/flyway/flyway/) for DB migrations. It runs kotlin migration scripts from [org.veo.forms.migrations](src/main/kotlin/org/veo/forms/migrations) when starting the service / spring test environment before JPA is initialized.

### Creating a migration
1. Modify DB model code (JPA entity classes).
2. `./gradlew bootRun`. The service might complain that the DB doesn't match the model but will silently generate the update DDL in `schema.local.sql`.
3. Copy SQL from `schema.local.sql`.
4. Create a new migration script (e.g. `src/main/kotlin/org/veo/forms/migrations/V3__add_fancy_new_columns.kt`) and let it execute the SQL you copied (see existing migration scripts).
5. Append a semicolon to every SQL command
6. Add some DML to your migration if necessary.

## License

verinice.veo is released under [GNU AFFERO GENERAL PUBLIC LICENSE](https://www.gnu.org/licenses/agpl-3.0.en.html) Version 3 (see [LICENSE.txt](./LICENSE.txt)) and uses third party libraries that are distributed under their own terms.
