# Smart Shipment Tracking backend

Java 21 / Spring Boot REST API backed by PostgreSQL. See
[ARCHITECTURE.md](../ARCHITECTURE.md) for the current data model, routes, and
transaction rules.

## Run locally

1. Install JDK 21 and PostgreSQL. Maven Wrapper is included.
2. In this directory, copy `.env.example` to `.env` and fill in your database
   connection settings. The file uses `KEY=value` syntax without surrounding quotes.
3. Create a database named `smart_shipment_tracking` (or change `DB_URL` to match
   your database).
4. For a **new, empty database**, apply the SQL scripts in this order:

   ```text
   database/create_customers.sql
   database/create_vehicles_and_locations.sql
   database/create_trips.sql
   database/create_employees_and_trip_employees.sql
   database/add_trip_vehicle.sql
   database/create_shipments.sql
   database/add_shipment_expected_delivery.sql
   database/create_tracking_events.sql
   database/merge_pr4_fields.sql
   ```

   Scripts are applied manually, for example through pgAdmin. For an existing
   database, apply only the missing scripts; they are not idempotent migrations.
5. Start the application from this directory:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On a Bash shell, use `./mvnw spring-boot:run`.

The default port is 8080. Startup validates the existing schema. Vehicles and
employees currently need database fixtures because their management routes are
not implemented.

`PUBLIC_TRACKING_BASE_URL` controls the URL placed in QR images. Its default is
`http://localhost:8080/api/tracking`, followed by the shipment's tracking number.
Set it to a customer-facing page URL when one is available.

## Tests

From this directory:

```powershell
.\mvnw.cmd test
```

Tests use temporary in-memory H2 databases in PostgreSQL mode, with generated
tables. They do not require `.env` or a running PostgreSQL server. The suite
includes integration tests for concurrent capacity reservations, duplicate
loading, version conflicts, and rollback when a history insert fails.

For PostgreSQL verification, point the test datasource at a separate test database:

```powershell
.\mvnw.cmd test "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/smart_shipment_tracking_test" "-Dspring.datasource.username=postgres"
```

Supply the test password with the `SPRING_DATASOURCE_PASSWORD` environment
variable. The suite uses `create-drop`; the database must be dedicated to tests,
and the user needs permission to create schemas. To run a single class, add
`-Dtest=ShipmentCapacityIntegrationTests`, for example.

The tests exercise service/persistence behavior and selected HTTP routes. Public
tracking and QR round-trip tests are still pending.
