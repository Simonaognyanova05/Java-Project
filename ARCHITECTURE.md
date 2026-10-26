# Smart Shipment Tracking — Technical Architecture

## 1. Purpose

A system for managing and tracking luggage during international transportation.
A customer creates a transport request, represented by one shipment in the first
version. Each shipment receives a unique tracking number and a QR code. Employees
manage its progress, and customers check its status and history using the tracking
number.

This document describes the intended implementation rather than existing features.

## 2. Architectural Approach

The application is a monolithic REST API with a layered architecture. Code is
organized by technical responsibility into controller, dto, mapper, model,
repository, service, and seeder packages. All layers run within one Spring Boot
application and share a PostgreSQL database.

Controllers receive HTTP requests, validate input, and call service methods.
Services enforce business rules, define transaction boundaries, and access data
through repositories. Repository interfaces extend JpaRepository. Spring Data JPA
provides their implementations, while Hibernate translates entity operations into
SQL.

DTO classes define the data accepted and returned by the API. They are separate
from JPA entities so that clients cannot modify internal fields or receive
internal object relationships. MapStruct mappers convert fields between DTOs and
entities. Services load related records and check business conditions.

Dependencies flow from controllers to services and from services to repositories.
Repositories have no HTTP responsibilities, services do not return HTTP responses,
and mappers do not query the database. Controllers do not call repositories
directly. Operations that update a shipment and its history share one service
transaction.

Request flow:

```text
HTTP → Controller → Service → Repository → PostgreSQL
            DTO ↔ Mapper ↔ Entity
```

| Package | Responsibility |
|---|---|
| controller | HTTP routes, input validation, and response status codes |
| dto | API request and response models |
| mapper | MapStruct conversion between DTOs and entities |
| model | JPA entities and enum types |
| repository | JpaRepository interfaces, derived queries, and JPQL |
| service | Business rules, transactions, tracking number generation, and QR generation |
| seeder | Sample data initialization in the dev profile only |
| exception | Consistent error handling through RestControllerAdvice |
| config | Security and infrastructure configuration |

Use constructor injection with `@RequiredArgsConstructor`. Microservices,
custom repository implementations, generic base controllers, and a service
interface for every class are unnecessary for this scope.

## 3. Project Structure

```text
smart-shipment-tracking/
├── pom.xml
├── .env.example
├── src/main/java/com/smartshipment/
│   ├── SmartShipmentTrackingApplication.java
│   ├── controller/
│   │   ├── CustomerController.java
│   │   ├── EmployeeController.java
│   │   ├── ShipmentController.java
│   │   ├── TrackingController.java
│   │   ├── TripController.java
│   │   └── VehicleController.java
│   ├── dto/
│   │   ├── CustomerDto.java
│   │   ├── EmployeeDto.java
│   │   ├── CreateShipmentRequest.java
│   │   ├── UpdateShipmentRequest.java
│   │   ├── ChangeShipmentStatusRequest.java
│   │   ├── AssignTripRequest.java
│   │   ├── ShipmentDto.java
│   │   ├── PublicTrackingDto.java
│   │   ├── TrackingEventDto.java
│   │   ├── TripDto.java
│   │   └── VehicleDto.java
│   ├── mapper/
│   │   ├── CustomerMapper.java
│   │   ├── EmployeeMapper.java
│   │   ├── ShipmentMapper.java
│   │   ├── TrackingEventMapper.java
│   │   ├── TripMapper.java
│   │   └── VehicleMapper.java
│   ├── model/
│   │   ├── Customer.java
│   │   ├── Employee.java
│   │   ├── Shipment.java
│   │   ├── ShipmentStatus.java
│   │   ├── TrackingEvent.java
│   │   ├── Trip.java
│   │   └── Vehicle.java
│   ├── repository/
│   │   └── ...Repository.java
│   ├── service/
│   │   ├── CustomerService.java
│   │   ├── EmployeeService.java
│   │   ├── ShipmentService.java
│   │   ├── TripService.java
│   │   ├── VehicleService.java
│   │   ├── TrackingNumberGenerator.java
│   │   └── QrCodeService.java
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── config/
│   └── seeder/
├── src/main/resources/
│   ├── application.properties
│   └── application-dev.properties
└── src/test/java/com/smartshipment/
    ├── service/
    ├── controller/
    └── integration/
```

## 4. Data Model

All entities use a `Long id` with `GenerationType.IDENTITY`.
Table and column names use snake_case.

| Model | Main fields |
|---|---|
| Customer | id, firstName, lastName, email (unique), phoneNumber, isDeleted |
| Employee | id, firstName, lastName, employeeNumber (unique), isDeleted |
| Shipment | id, trackingNumber (unique, immutable), customer, trip (optional), status, description, weightKg, pickupAddress, deliveryAddress, createdAt, expectedDeliveryAt, deliveredAt, version |
| TrackingEvent | id, shipment, previousStatus (nullable only for CREATED), newStatus, occurredAt, employee (nullable only for CREATED), note |
| Trip | id, origin, destination, departureAt, expectedArrivalAt, vehicle, employees, version, isDeleted |
| Vehicle | id, registrationNumber (unique), capacityKg, isDeleted |

Relationships:

- Customer 1 → N Shipment.
- Trip 1 → N Shipment; a shipment may initially have no assigned trip.
- Vehicle 1 → N Trip.
- Shipment 1 → N TrackingEvent.
- Employee 1 → N TrackingEvent.
- Employee N ↔ M Trip: a trip has a crew, and an employee can work on multiple trips.

Implement the crew relationship with @ManyToMany on Trip.employees and a
@JoinTable named trip_employees with trip_id and employee_id as a composite
primary key or unique pair. An optional Employee.trips collection uses
mappedBy = "employees". Do not cascade removal between employees and trips.
TripService assigns and removes active crew members transactionally. A trip
must have at least one active crew member before a shipment becomes IN_TRANSIT.
Crew membership represents assignment, while TrackingEvent.employee records
who actually performed a status change. Keep both relationships.

Use `@ManyToOne(fetch = FetchType.LAZY)` and explicit foreign keys.
Inverse collections are optional and are not required for every relationship.
Do not use `CascadeType.REMOVE` or `orphanRemoval` for shipments or history.
Do not serialize entities directly into HTTP responses.

`ShipmentStatus` is an enum persisted with `@Enumerated(EnumType.STRING)`.
Timestamps use `Instant` and are returned in ISO 8601 format.
Weight and capacity use `BigDecimal`, both measured in kg.
Addresses, origin, and destination are text fields in the first version.
Location is implemented as a standalone entity with id, country, city, and address. Its relationships to trips and shipments will be added separately.

## 5. Shipment Lifecycle

```text
CREATED → ACCEPTED → LOADED → IN_TRANSIT → DELIVERED
```

| Current status | Allowed next status | Conditions |
|---|---|---|
| CREATED | ACCEPTED | An employee has physically received the shipment |
| ACCEPTED | LOADED | An active trip and vehicle are assigned, with sufficient capacity |
| LOADED | IN_TRANSIT | Transportation begins and the assigned trip has at least one active crew member |
| IN_TRANSIT | DELIVERED | An employee confirms successful delivery |
| DELIVERED | None | Final status |

Skipping statuses, moving backward, or assigning the same status again is not
allowed. Status changes go through a dedicated service method. General shipment
updates do not accept status, trackingNumber, or deliveredAt.

On creation, the server sets CREATED and records an initial TrackingEvent.
On delivery, it sets deliveredAt. In the first version, the employee's DELIVERED
event serves as delivery confirmation. Signatures, photos, and PIN confirmation
are future extensions.

## 6. Transactions and Concurrent Updates

`ShipmentService.create()` and `changeStatus()` use `@Transactional`.

A status change performs the following within one transaction:

1. Load the shipment and check its expected version.
2. Validate the transition and its business conditions.
3. Update the status and, when applicable, deliveredAt.
4. Create a TrackingEvent containing the timestamp and current employee.
5. Commit both changes together or roll back the entire operation.

Shipment has a `@Version Long version` field. Change requests include
`expectedVersion` from a previous read. The service checks that value, while
Hibernate optimistic locking also detects changes made after the entity was
loaded. Conflicts return `409 Conflict` without automatic retries.

`@Transactional` alone does not protect constraints spanning several records.
Capacity checks and loading different shipments onto the same trip are serialized
using a pessimistic write lock on Trip. Assignment, reassignment, and loading
follow the same locking order. The capacity calculation sums the weights of all
shipments assigned to the trip.

A shipment can change trips only while CREATED or ACCEPTED. Its trip cannot
change after LOADED. Deactivating a trip or vehicle is rejected if it serves
undelivered shipments. These operations use the same shared locking protocol as
assignment.

## 7. DTOs, Mapping, and Validation

The API uses separate DTO classes and MapStruct mappers for conversion to and
from JPA entities. Request DTOs declare Bean Validation constraints and are
accepted with `@Valid`.

- Names and addresses: `@NotBlank` and appropriate `@Size` limits.
- Email: `@NotBlank`, `@Email`, and `@Size(max = 255)`.
- Phone number: optional; validate international format when provided.
- Weight and capacity: required positive values.
- Related record identifiers: `@NotNull` and `@Positive` when required.
- expectedDeliveryAt: a future timestamp when creating a shipment.
- departureAt must precede expectedArrivalAt.
- ChangeShipmentStatusRequest: newStatus and expectedVersion.

Creation mappers ignore generated fields, version, status, and JPA relationships.
Services load related records by identifier. Response DTOs represent
relationships through identifiers or small nested DTOs without recursive nesting.

## 8. REST API

| Method | Route | Purpose |
|---|---|---|
| GET / POST | /api/customers | List / create customers |
| GET / PUT / DELETE | /api/customers/{id} | Read / update / deactivate a customer |
| GET / POST | /api/employees | List / create employees |
| GET / PUT / DELETE | /api/employees/{id} | Read / update / deactivate an employee |
| POST | /api/shipments | Create a shipment |
| GET | /api/shipments | List shipments with pagination and filters |
| GET / PUT | /api/shipments/{id} | Read / update a shipment before ACCEPTED |
| PATCH | /api/shipments/{id}/status | Perform an allowed status transition |
| PUT | /api/shipments/{id}/trip | Assign or change a trip |
| GET | /api/shipments/{id}/history | Retrieve history for authorized staff |
| GET | /api/shipments/{id}/qr | Generate a PNG QR code |
| GET | /api/tracking/{trackingNumber} | Retrieve limited public tracking information |
| GET / POST | /api/trips | List / create trips |
| GET / PUT / DELETE | /api/trips/{id} | Read / update / deactivate a trip |
| PUT / DELETE | /api/trips/{id}/employees/{employeeId} | Assign / remove a crew member |
| GET | /api/shipments/overdue | List overdue shipments using a custom @Query |
| GET / POST | /api/vehicles | List / create vehicles |
| GET / PUT / DELETE | /api/vehicles/{id} | Read / update / deactivate a vehicle |

Creation returns 201 Created with a Location header. Successful deactivation
returns 204. Invalid input returns 400, missing records return 404, and business
or version conflicts return 409. Unauthenticated and forbidden requests return
401 and 403 respectively.

GlobalExceptionHandler returns a consistent ProblemDetail format, including
field validation errors. Unique constraint violations are mapped to 409.
Preliminary existsBy checks improve error messages, but database constraints
remain mandatory to protect against concurrent requests.

## 9. Tracking Numbers, QR Codes, and Access

Authentication is an optional extension. The account-based rules below apply when
that extension is implemented. Without authentication, the application is a local
demonstration: employee identity is explicitly supplied and validated as an
active employee, and customer ownership is not claimed to be enforced. Do not
publish unrestricted management endpoints as a production service.

Tracking numbers are generated on the server with sufficient randomness, for
example UUIDs, and protected by a UNIQUE constraint. Do not expose sequential
database identifiers as tracking numbers. In the unlikely event of a collision,
retry creation with a new number in a new transaction, using a bounded retry count.

The QR code contains the tracking number. Its image is generated on request
rather than stored as a BLOB. A future customer-facing page may use a configurable
tracking URL instead.

PublicTrackingDto contains the tracking number, status, expected and actual
delivery timestamps, and a history of statuses and timestamps. It excludes
addresses, phone numbers, email addresses, employee names, and internal notes.

Customer and Employee are business models rather than user accounts.
Use Spring Security and a separate Account model with a CUSTOMER, EMPLOYEE, or
ADMIN role and a relationship to the corresponding business model. Store
password hashes. Customers manage only their own requests. The employee recorded
in a TrackingEvent comes from the authenticated user rather than an employeeId
supplied in the request body. Administrators manage employees and vehicles.

## 10. Deletion and History

Customer, Employee, Trip, and Vehicle use isDeleted for deactivation.
Repository queries filter inactive records in the database rather than using
Java streams. Existing historical relationships are preserved.

Shipment and TrackingEvent have no public hard-delete operation. History is
appended only through business operations and has no PUT or DELETE endpoint.
Shipment cancellation and restoration of deactivated records are outside the
first version and require explicit business rules before implementation.

## 11. Technologies and Configuration

The project uses Java 21 and Spring Boot 4.1.1 as specified in pom.xml.
Maven manages dependencies, compilation, and test execution.

- Maven and Maven Wrapper.
- Spring Web MVC, Data JPA, and Bean Validation.
- PostgreSQL and Hibernate.
- Lombok and MapStruct with annotation processors and lombok-mapstruct-binding.
- OpenAPI/Swagger UI through a springdoc version compatible with the selected Boot version.
- A QR library such as ZXing.
- Spring Security for roles and record ownership.
- JUnit, Mockito, and PostgreSQL Testcontainers for integration testing.

DB_URL, DB_USERNAME, and DB_PASSWORD are supplied through environment variables
or a local git-ignored .env file. Include an .env.example without secrets.

Local development may use spring.jpa.hibernate.ddl-auto=update.
Shared environments use Flyway migrations and ddl-auto=validate.
Set spring.jpa.open-in-view=false and map lazy relationships to DTOs within
service transactions. Use pagination and targeted fetch queries for lists to
avoid N+1 queries.

Seeders use @Profile("dev"), run in a defined order, and are idempotent by stable
unique keys. They do not rely solely on count() > 0. Sample shipments are created
through business operations so they have valid history.

## 12. Technical Invariants

- A new shipment has a unique tracking number and exactly one initial event.
- Each allowed transition updates the status and appends one event atomically.
- A rejected transition changes neither the shipment nor its history.
- Two updates using the same expectedVersion produce one success and one 409.
- Two concurrent loading operations cannot exceed trip capacity.
- A shipment cannot be loaded without a valid trip and vehicle.
- DELIVERED sets deliveredAt and allows no further transitions.
- If authentication is implemented, customers cannot edit another customer's shipment or change shipment status.
- Public tracking does not expose personal information.
- The QR code decodes to the shipment's tracking number.
- Deactivated records are unavailable for new assignments.
- History remains available after an employee is deactivated.
- Integration tests verify rollback, unique constraints, and concurrent updates
  against a real PostgreSQL instance.

## 13. Custom Repository Query

ShipmentRepository must contain an explicit JPQL @Query for overdue shipments:

```java
@Query("""
    SELECT s FROM Shipment s
    WHERE s.expectedDeliveryAt < :now
      AND s.status <> :deliveredStatus
    ORDER BY s.expectedDeliveryAt ASC, s.id ASC
    """)
List<Shipment> findOverdueShipments(
    @Param("now") Instant now,
    @Param("deliveredStatus") ShipmentStatus deliveredStatus,
    Pageable pageable
);
```

The service supplies the current time from an injected Clock and passes
ShipmentStatus.DELIVERED. Pageable bounds the result size. The query powers
GET /api/shipments/overdue and supports operational follow-up on delayed luggage.
Test that overdue undelivered shipments are included, delivered shipments are
excluded, and shipments whose deadline has not passed are excluded.

## 14. Integration Test Design

Integration tests exercise business operations and persistence rather than only application startup.
Use @SpringBootTest with a PostgreSQL Testcontainer and real repositories.
Do not mock persistence in these tests.

Shipment status transition scenario:

1. Persist an active customer, employee, and a CREATED shipment with its initial event.
2. Execute CREATED → ACCEPTED through ShipmentService.
3. Reload from the database and verify ACCEPTED and exactly one additional event,
   including its employee and timestamp.
4. Attempt ACCEPTED → DELIVERED.
5. Verify rejection, unchanged status, and no additional event.

Also implement the concurrency scenario central to the project:

1. Read the same shipment version for two simulated employee requests.
2. Execute updates in separate transactions, using synchronization barriers
   rather than timing-based sleeps to coordinate competing operations.
3. Verify one successful commit and one version conflict.
4. Verify the final status and exactly one new history event.
5. Verify through an API test that a version conflict maps to HTTP 409.

For capacity protection, test two competing assignments that individually fit
the remaining capacity but exceed it together. At most one may commit.
Because assigned shipment weights reserve capacity, reject excess assignments
before loading as well as rechecking capacity during loading.

Use independent committed setup and verification transactions for concurrency
tests. A transaction surrounding the entire test must not hide fixtures from
worker transactions. Add a rollback test showing that a failure while persisting
history does not leave the shipment status changed.

## 15. OpenAPI Specification

Document endpoint purpose, request and response schemas, required fields,
status enum values, pagination, and representative examples. Include 400, 404,
and 409 responses and explain the expectedVersion field. Document crew assignment
and overdue shipment search. If security is implemented, document authentication
and 401/403 responses as well. Include instructions in README for opening Swagger
UI and the generated OpenAPI specification.


## 16. Local database credentials and secrets

Store database connection settings and any future secrets (passwords, API keys,
and signing keys) in smart-shipment-tracking/.env. Never commit real secrets.
The .env file and environment-specific .env files are excluded from Git;
.env.example contains only non-secret sample values and an empty password.

Before starting the application, fill in DB_URL, DB_USERNAME, and DB_PASSWORD
in .env using .env.example as a reference. The password is intentionally blank
in the template and must be supplied locally.

application.properties imports .env as a Java properties file, supporting startup
from either the repository root or the smart-shipment-tracking directory. Use
KEY=value syntax without surrounding quotes. Environment variables can override
these settings. Keep new secret values out of source code and documentation.

Create the PostgreSQL database and customers table before startup. Hibernate uses
ddl-auto=validate, so startup checks the schema rather than creating tables.

## 17. Vehicle and Location entities

Vehicle is in com.smartshipment.vehicle.entity and maps to vehicles. It stores a
unique registrationNumber (30 characters), positive capacityKg as BigDecimal
(NUMERIC(12,2), kg), and is_deleted for deactivation. Bean Validation rejects
missing or invalid values when the entity is persisted.

Location is in com.smartshipment.location.entity and maps to locations. Country
and city are required strings of at most 100 characters; address is required and
at most 255 characters. Both entities use identity-generated Long identifiers.

Before restarting with ddl-auto=validate, run the complete SQL script
smart-shipment-tracking/database/create_vehicles_and_locations.sql in pgAdmin
against smart_shipment_tracking. It creates both tables in a transaction. It is
a manual setup script, not an automatically applied migration.

This step adds the entities only. Vehicle and Location repositories, DTOs,
controllers, and Trip relationships are outside this step. Trip currently retains
its text origin and destination fields.
## 18. Trip management implementation

TripController exposes GET/POST /api/trips, GET/PUT/DELETE /api/trips/{id},
and PUT/DELETE /api/trips/{id}/employees/{employeeId}. Creation returns 201
with a Location header; deletion and crew removal return 204. Unknown or
inactive resources return 404, while invalid request fields return 400.

TripRequest requires origin, destination, departureAt, expectedArrivalAt, and
vehicleId. Arrival must be strictly after departure. TripResponse additionally
contains vehicleId and sorted employeeIds, with no entity serialization.
TripMapper leaves relationship resolution to TripService.

TripService uses transactions and locks the active trip row for updates,
deactivation, and crew changes. Lists and reads map relationships inside a
read-only transaction. Trips are deactivated using is_deleted, and queries
exclude them. Crew assignment is idempotent and accepts active employees only;
removal also permits inactive employees so historical assignments can be cleaned
up. Removing membership does not delete the employee.

Run smart-shipment-tracking/database/add_trip_vehicle.sql once in pgAdmin
against smart_shipment_tracking before restarting. This adds the nullable
vehicle_id foreign key, preserving existing trips. New API creates and updates
require an active vehicle. Vehicles and employees currently need to be created
in the database; their management endpoints are not implemented yet.

At the time of the TripController step, shipment assignment, capacity enforcement, and rejection of trip changes serving
undelivered shipments require the upcoming Shipment integration. They are not
implemented by this step. Location remains standalone and route endpoints are
still text fields.
## 19. Shipment assignment implementation

PUT /api/shipments/{id}/trip accepts tripId and expectedVersion. It returns the
shipment ID, tracking number, status, weightKg, assigned tripId, and current
version. GET /api/trips/{id}/shipments lists assigned shipments without exposing
customer details. Invalid inputs return 400, missing records 404, and stale
versions, invalid states, inactive targets, or insufficient capacity 409.

ShipmentAssignmentService permits assignments and reassignments only in CREATED
or ACCEPTED. It locks the shipment first, then both old and new trip rows in
ascending ID order. All future assignment and loading operations must follow
this order. All weights assigned to a trip reserve capacity, including shipments
not yet loaded. Repeating the same assignment does not count the weight twice.
Trip updates check the replacement vehicle capacity under the same trip lock;
trip deactivation rejects undelivered assigned shipments.

The repository previously had no Shipment module. A minimal transport model now
provides id, UUID trackingNumber, customer, optional trip, status, weightKg,
createdAt, and version. Run database/create_shipments.sql manually against
smart_shipment_tracking before restart. This is not the full planned Shipment
module: creation endpoints, initial TrackingEvent, status transitions, expected
delivery fields, addresses, QR generation, and public tracking remain separate
work. The constructor is a persistence foundation, not a completed shipment
creation business operation. Assignment does not change status or create a
status event. Existing or externally created shipment records can be assigned.

Assignment and rejection scenarios are integration-tested with H2. PostgreSQL capacity concurrency verification is documented in section 20. Loading remains a separate task.
## 20. Transactional capacity verification

Shipment assignment and trip modification transactions explicitly use
READ_COMMITTED isolation. Assignment holds the shipment lock, then the old and
new trip locks in ascending ID order until commit or rollback. Under these locks,
the sum of all assigned weights plus the incoming weight must not exceed the
vehicle capacity. The same-trip case does not add the weight again. BigDecimal
preserves decimal kg values and equality with the capacity is allowed. A rejected
reassignment leaves the old assignment and shipment version unchanged.

ShipmentCapacityIntegrationTests uses independent committed setup, worker, and
verification transactions. Two workers synchronize with a CyclicBarrier after
locking different shipments, then compete to move from distinct source trips
into one target. With 20 kg reserved and two incoming 60 kg shipments against a
100 kg capacity, exactly one commits; the rejected shipment retains its source
trip and version. A second test verifies 33.33 + 66.67 kg exactly fills 100 kg,
and an extra 0.01 kg is rejected without persistence changes.

The tests have passed against local PostgreSQL 18.6. To repeat from the project
directory with local credentials configured in .env:

    .\mvnw.cmd -Dtest=ShipmentCapacityIntegrationTests test

This test class generates a unique capacity_test_<UUID> schema, creates its own
tables, and drops them on context shutdown. It does not use public for test data.
The database user needs permission to create schemas. Docker is not required.
No application schema changes or additional manual SQL are needed for this step.

This verifies concurrent assignment/reassignment capacity protection. Shipment
loading, status/history changes, and their integration test are separate tasks.
Future loading and weight-changing operations must obey the same locking protocol.
## 21. Overdue shipment query

Shipment now has an optional expectedDeliveryAt Instant. Existing records without
an expected deadline are excluded from overdue reporting. Run the complete
smart-shipment-tracking/database/add_shipment_expected_delivery.sql script in
pgAdmin against smart_shipment_tracking before restarting. It adds the nullable
expected_delivery_at column and an index on (expected_delivery_at, id).

ShipmentRepository.findOverdueShipments uses an explicit JPQL @Query with
expectedDeliveryAt < :now and status <> :deliveredStatus. Ordering is by deadline
ascending, then shipment ID ascending. Equality with now is not overdue.
OverdueShipmentService supplies Instant.now(clock) using an injected UTC Clock
and ShipmentStatus.DELIVERED; mapping runs inside a read-only transaction.

GET /api/shipments/overdue?page=0&size=20 returns a JSON array of id,
trackingNumber, status, expectedDeliveryAt, and optional tripId. Page is zero-based;
size is limited to 1..100. Invalid pagination returns 400, and no matches return
an empty array. Response DTOs do not include customer details. This endpoint is
an operational report; it does not send notifications or change shipment status.

Integration tests use a fixed Clock and cover all undelivered statuses, exclusion
of delivered/future/equal-time/missing-deadline shipments, stable ordering,
pagination, empty results, and invalid page sizes. The future shipment creation
API should require a future deadline; existing constructor-based fixtures remain
compatible. No deadline update API is added in this step.
## 22. Concurrent loading integration test

ShipmentLoadingService.load(shipmentId, employeeId, expectedVersion) implements
only ACCEPTED -> LOADED. It uses one READ_COMMITTED transaction, locks shipment
then trip, checks the expected version, active trip, active vehicle, and active
employee, and rechecks total reserved trip weight. Loading does not add reserved
weight again. The status/version update and one TrackingEvent commit together.
The event records ACCEPTED, LOADED, an Instant from Clock, and the employee.
There is no cascading deletion of history or related records.

ShipmentLoadingIntegrationTests exercises real repositories and service methods.
Fixtures, each worker, and final verification use separate committed transactions.
A CyclicBarrier starts the workers together without timing-based sleeps. Cases:

- Two employees loading one shipment: exactly one succeeds, one receives a 409
  service exception, the version increments once, and exactly one loading event
  identifies the successful employee.
- Different shipments totaling exactly the reserved capacity both load successfully.
- An intentionally oversubscribed legacy fixture is rejected without status,
  version, or history changes.
- A database constraint rejecting the history INSERT causes the entire operation
  to roll back, including shipment status and version. The constraint is temporary
  and scoped to this test's shipment in the isolated test schema.

The four tests passed against local PostgreSQL 18.6. Repeat from the project folder:

    .\mvnw.cmd -Dtest=ShipmentLoadingIntegrationTests test

Like the capacity test, this class generates a unique loading_test_<UUID> schema,
creates and drops its own tables, and does not store test data in public. Local
credentials come from .env; the user needs permission to create schemas.

Before normal application startup, run database/create_tracking_events.sql once
in pgAdmin against smart_shipment_tracking. The application now maps tracking_events
and ddl-auto=validate requires that table. No changes to existing shipment rows
are needed.

This step adds a service-level loading operation and its integration tests, not
a loading HTTP endpoint. Acceptance, other lifecycle transitions, the initial
CREATED event, and the complete tracking API remain separate lifecycle work.
The test prepares ACCEPTED fixtures directly rather than claiming to test those
unimplemented operations. The 409 behavior is verified at the service boundary;
an HTTP mapping test belongs with the future status controller.
## 23. PR #4 local merge resolution

The develop branch was reconciled with main. Main remains the canonical contract
for Trip fields (origin, destination, departureAt, expectedArrivalAt, vehicleId),
Instant timestamps, active-record filtering, optimistic versions, pessimistic
locking, transactional capacity checks, history, and all existing tests. PR #4's
separate unrestricted TripStatus lifecycle and duplicate trip tracking number
were not adopted; adding a trip lifecycle requires explicit transition rules.
The unused enum and accidentally committed .DS_Store were removed.

Compatible PR additions:

- POST /api/shipments creates a shipment with customerId, recipientName,
  deliveryAddress, weightKg, and a future expectedDeliveryAt. The server generates
  the tracking number; a client-supplied non-null trackingNumber is rejected.
  Customer lookup, shipment persistence, and one initial CREATED event share a
  transaction. GET /api/shipments/{id} resolves the returned Location header.
- Existing main assignment routes remain unchanged. The older PR route
  PUT /api/shipments/{shipmentId}/trip/{tripId}?expectedVersion=0 delegates to the
  same assignment service. The version parameter is required; this route cannot
  bypass capacity, status, active-record, or version checks.
- GET /api/shipments/trip/{tripId} is retained as a staff-oriented detailed list.
  GET /api/trips/{tripId}/shipments still returns the original limited projection.
- Employee retains employeeNumber and deactivation, and gains optional unique
  email and phone. The inverse trips collection is synchronized by both sides'
  add/remove helpers, without cascade deletion.
- Vehicle retains registrationNumber, positive capacityKg, and deactivation;
  optional make, model, and vehicleType are added. PR licensePlate maps
  conceptually to registrationNumber rather than introducing a second identifier.
- Location retains country, city, and address. Optional latitude, longitude,
  recordedAt (Instant), and vehicle association support GPS information, with
  Bean Validation for latitude/longitude ranges.

For an existing main database, apply database/merge_pr4_fields.sql once after all
previous database scripts and before normal application startup. It only adds
nullable columns and constraints/indexes and preserves existing records. The
merge process itself does not apply this SQL to the public schema.

Example creation request:

    POST /api/shipments
    {
      "customerId": 1,
      "recipientName": "Maria Petrova",
      "deliveryAddress": "Vienna 1",
      "weightKg": 10.00,
      "expectedDeliveryAt": "2030-01-01T12:00:00Z"
    }

ShipmentCreationIntegrationTests covers creation/history atomicity, generated
tracking numbers, request validation, the protected legacy assignment route,
metadata persistence, and the inverse crew relationship. It uses a generated
pr4_test_<UUID> schema like the loading and capacity tests. All 27 tests passed
with an isolated H2 database during merge verification. Production credentials
remain in the ignored .env file. No remote publication is part of this merge.
Post-merge verification also passed 11 PostgreSQL 18.6 integration tests across
ShipmentCreationIntegrationTests, ShipmentCapacityIntegrationTests, and
ShipmentLoadingIntegrationTests in isolated schemas; no public data was changed.
