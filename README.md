# Student Management · AWS DevSecOps Lab

A ready-made Java 21 / Spring Boot 4.1.1 application so the project can focus on AWS infrastructure, CI/CD, deployment strategies, monitoring, and rollback.

## Run locally with Docker

Install Docker Engine and the Docker Compose plugin. Java and Maven are **not** required on the host for this option.

```bash
cp .env.example .env
```

Edit `.env` and replace both password placeholders with different local passwords. Then:

```bash
docker compose up --build -d
```

Open http://localhost:8080. The page supports adding, viewing, editing, and deleting students, and displays health and release information. Startup may take a minute while MySQL initializes.

```bash
docker compose logs -f app
curl --fail http://localhost:8080/actuator/health
python3 scripts/smoke-test.py
```

Expected health response: `{"status":"UP"}`. The smoke test creates synthetic data, exercises CRUD and validation, and deletes its record.

Stop containers while keeping data:

```bash
docker compose down
```

Compose creates a persistent, project-scoped MySQL volume automatically. `docker compose down -v` deletes that volume and all its records; use only when intentionally resetting disposable demo data. The database is not exposed to the host in the normal configuration.

## Tests without Docker

Install JDK 21. The Maven wrapper downloads Maven automatically.

```bash
./mvnw clean verify
```

The tests use H2 in MySQL compatibility mode, exercising the application, migration, validation, and health endpoints. CI also runs the same suite against real MySQL 8.4 and smoke-tests the built container. H2 alone is not proof of MySQL compatibility.

On Windows use `mvnw.cmd clean verify`.

## API

| Method | Path | Result |
| --- | --- | --- |
| GET | `/` | Demo web page |
| GET | `/students` | Student list |
| GET | `/students/{id}` | Student, or 404 |
| POST | `/students` | Create student, 201 |
| PUT | `/students/{id}` | Update student, or 404 |
| DELETE | `/students/{id}` | Delete student, 204, or 404 |
| GET | `/actuator/health` | Application and database health |
| GET | `/actuator/health/liveness` | Process liveness |
| GET | `/actuator/health/readiness` | Readiness including database |
| GET | `/actuator/info` | Application release identifier |

Example:

```bash
curl --fail -H 'Content-Type: application/json' \
  -d '{"name":"Vijay","email":"vijay@example.com","course":"AWS DevOps"}' \
  http://localhost:8080/students
```

All three fields are required and limited to 255 characters; email must have a valid format. Invalid input returns 400 with field errors. No unique-email requirement is imposed.

## Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC connection string | `jdbc:mysql://localhost:3306/studentdb` |
| `DB_USERNAME` | Application database user | `studentuser` |
| `DB_PASSWORD` | Database password | Required |
| `PORT` | HTTP port | `8080` |
| `APP_VERSION` | Release shown on page and info endpoint | `local` |
| `MYSQL_ROOT_PASSWORD` | MySQL initialization password for Compose | Required by Compose |

`SPRING_DATASOURCE_*` can also override Spring's datasource settings. Keep credentials outside Git; later AWS stages should inject them through Secrets Manager or an equivalent service. Compose disables database TLS only for its local private container network. For AWS, configure a verified TLS database connection.

The container runs as UID 10001, builds in a separate Maven stage, and supports graceful shutdown. Set `APP_VERSION` to a commit SHA or image tag to distinguish deployments. Build tests run separately in CI before the Docker image is built.

## Database migrations and existing data

Flyway owns schema changes in `src/main/resources/db/migration`. Hibernate validates the resulting schema rather than changing it automatically. New databases work without manual SQL.

The normal Compose configuration uses a **new project-scoped volume**. It does not attach or delete the older external `student-mysql-data` volume, so existing local records will not appear automatically.

To reuse an existing database, take a backup first and verify its `students` table matches V1 (`id BIGINT` identity primary key; `name`, `email`, and `course` as `VARCHAR(255)`). Only for that initial adoption, start with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=0`. V1 uses `CREATE TABLE IF NOT EXISTS`; it will not repair incompatible existing tables. Remove the adoption settings after success. Do not blindly baseline an unknown database. Application rollback does not reverse database migrations.

## CI and next AWS work

GitHub Actions builds and tests with Java 21, verifies against MySQL 8.4, builds the image, runs HTTP smoke tests, and checks data survives an application restart. Test reports are uploaded as an artifact. Tests use synthetic student data.

The AWS pipeline, ECR publishing, ECS deployment, Terraform, security scanning, CloudWatch alarms, canary traffic shifting, and automatic rollback remain the next project phases. This commit prepares the application foundation; it does not create AWS resources.

This is a portfolio lab without authentication. Use synthetic records and restricted access. Add authentication and authorization before using real student information or offering public write access.
