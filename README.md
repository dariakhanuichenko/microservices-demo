# Microservices Demo (Kafka + Spring Boot)

Four Spring Boot services in one monorepo, talking to each other only through
Kafka events. Each service keeps its own git history (imported with
`git subtree`), so `git log -- order-service` still shows how that service grew.

## Event flow

```
POST /create
     |
     v
order-service ──orders.created──> payment-service ──payments.completed──> user-service ──user.updated──> inventory-service
  (8080)                              (8081)                                (8082)                          (8083)
```

`order-service` does not publish to Kafka directly. It writes the order and an
outbox row in one transaction; a scheduler (`OrderEventProducer`, every 5s)
picks unprocessed rows up and publishes them. That is the transactional outbox
pattern: the order and the intent to publish either both commit or neither does.

`payment-service` retries a failed message three times (`@RetryableTopic`) and
then parks it in `orders.created.dlq`, which it also listens to for logging.

## Layout

```
pom.xml                 parent: dependency versions + module list
Dockerfile              one build for all four services (--build-arg SERVICE=...)
docker-compose.yml      Kafka, Zookeeper, Postgres and the four services
.env.example            copy to .env to change DB credentials
order-service/          REST entrypoint, Postgres, outbox publisher
payment-service/        consumes orders.created, produces payments.completed
user-service/           consumes payments.completed, produces user.updated
inventory-service/      consumes user.updated (end of the chain)
```

## Run everything

```bash
cp .env.example .env      # optional, defaults work as-is
docker compose up --build
```

First run takes a few minutes: each image builds its service with Maven inside
the container. Startup order is enforced by health checks, so the services wait
for Postgres and Kafka rather than crash-looping.

Create an order:

```bash
curl -X POST "http://localhost:8080/create?amount=42"
```

Then watch the chain:

```bash
docker compose logs -f payment-service user-service inventory-service
```

Within ~5 seconds (one outbox poll) you should see the order reach payment,
then user, then inventory.

## Run one service from the IDE

Kafka and Postgres are published on the host, so the defaults in each
`application.yml` (`localhost:9092`, `localhost:5432`) work when you start only
the infrastructure:

```bash
docker compose up -d zookeeper kafka kafka-init postgres
```

Then run `OrderApplication` (or any other) from IntelliJ as usual.

## Build without Docker

```bash
mvn clean package             # all four
mvn -pl order-service -am package   # just one, with the parent
java -jar order-service/target/order-service-1.0-SNAPSHOT.jar
```

## Database

`order-service` owns `ordersdb`. Schema is managed by Flyway
(`order-service/src/main/resources/db/migration`), and Hibernate is set to
`ddl-auto: validate` so it verifies the entities match rather than silently
rewriting the schema. To add a table, add `V3__whatever.sql` — never edit an
applied migration.

## Kafka listeners

The broker advertises two listeners:

| from | address |
|---|---|
| a container in this compose network | `kafka:29092` |
| your machine (IDE, CLI tools) | `localhost:9092` |

Services in compose get `KAFKA_BOOTSTRAP_SERVERS=kafka:29092` from
`docker-compose.yml`; everything else falls back to `localhost:9092`.
