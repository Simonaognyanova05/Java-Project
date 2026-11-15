# Smart Shipment Tracking

## Overview

The backend is a Spring Boot REST application for managing transport trips and
shipments. Staff create shipments, assign them to trips, and record status
changes. Customers can look up a limited shipment history by tracking number.

The application uses Java 21, Spring Boot 4.1.1, Spring Data JPA, Bean Validation,
PostgreSQL, and ZXing for QR images. Maven Wrapper is included.

This document describes the current implementation. Setup instructions are in
[the backend README](smart-shipment-tracking/README.md); the original design and
development notes are in [the architecture history](docs/architecture-history.md).

## Code organization

Packages under `com.smartshipment` are organized by feature: `customer`,
`employee`, `location`, `shipment`, `tracking`, and `trip`, plus `vehicle` and
shared configuration in `config`.

Within each feature:

- Controllers define routes and validate request bodies and path parameters.
- Services enforce business rules and define transaction boundaries.
- Repositories provide persistence queries and row-locking operations.
- DTO records define the HTTP payloads. Entities are not serialized directly.
- Mapper components perform explicit Java field mapping.

Services use constructor injection. They report missing records and business
conflicts through `ResponseStatusException`. Error responses otherwise use Spring
MVC and Spring Boot defaults; there is no application-wide exception handler.

## Data model

All entities use identity-generated `Long` identifiers. Times use `Instant`;
shipment weight and vehicle capacity use `BigDecimal` in kilograms.

| Entity | Role |
|---|---|
| Customer | Sender details, unique email, and contact phone |
| Employee | Staff details, unique employee number, optional email/phone, deactivation flag |
| Vehicle | Unique registration number, positive capacity, optional make/model/type, deactivation flag |
| Trip | Origin, destination, schedule, optional vehicle, crew, optimistic version, deactivation flag |
| Shipment | UUID tracking number, customer, optional trip, status, weight, recipient/address, timestamps, optimistic version |
| TrackingEvent | Shipment, previous/new status, occurrence time, optional employee |
| Location | Country/city/address and optional GPS coordinates, recording time, and vehicle |

A customer has many shipments; a trip can carry many shipments. Trips and
employees have a many-to-many crew relationship through `trip_employees`.
Both sides of crew membership are maintained by entity helpers. Related
customers, employees, trips, and vehicles are not cascade-deleted.

Trips are deactivated through the API. Customers currently use physical deletion,
subject to database foreign keys. Shipments and tracking events have no delete
endpoint. Employee and vehicle management endpoints are not implemented.

## Shipment creation and history

`ShipmentService.create` loads the customer, generates a shipment through the
mapper, and persists its initial `CREATED` event in one transaction. Tracking
numbers are generated with UUIDs and protected by a database unique constraint.
Requests cannot supply their own tracking number.

The service obtains one creation timestamp from the injected `Clock` and uses it
for both `Shipment.createdAt` and the initial event. Status-change and overdue
services use the same clock abstraction. The two-argument entity constructor
remains available for direct construction and uses the system time.

Events are written by creation, loading, and lifecycle services. History queries
order by occurrence time and then event ID. A failed history insert rolls back
the shipment operation as well.

## Status changes

The entity permits only the next step in this sequence:

```text
CREATED -> ACCEPTED -> LOADED -> IN_TRANSIT -> DELIVERED
```

Skipping a step, moving backwards, and repeating the current status are rejected.
`ShipmentLifecycleService` validates the expected version and active employee
before changing status and appending an event.

Loading is implemented separately in `ShipmentLoadingService.load`. It requires
an `ACCEPTED` shipment, an active trip and vehicle, sufficient reserved capacity,
an active employee, and the expected shipment version. It records the `LOADED`
event in the same transaction.

Loading currently has no HTTP route. The status endpoint rejects requests for
`LOADED`, so the complete lifecycle is not yet available over HTTP. The
`IN_TRANSIT` transition does not yet check for an active crew, and delivery is
represented by its history event rather than a separate `deliveredAt` column.

## Assignment, capacity, and concurrent updates

A shipment can change trips only while `CREATED` or `ACCEPTED`. The target trip
must have an active vehicle. All assigned weights reserve capacity, including
shipments that have not been loaded. Reassigning to the same trip does not add
the weight twice. Loading rechecks the total without reserving it again.

Assignment and loading use `READ_COMMITTED` transactions and a shared lock order:

1. Lock the shipment row with `PESSIMISTIC_WRITE`.
2. Lock the affected trip rows. Reassignment locks old and new trip IDs in
   ascending order to avoid opposite lock ordering.
3. Check capacity and update the shipment before committing.

Trip updates, deactivation, and crew changes also lock the trip row. A replacement
vehicle must fit the reserved shipment weight. A trip with undelivered shipments
cannot be deactivated.

`Shipment` and `Trip` have JPA `@Version` fields. Shipment assignment, loading,
status changes, and trip updates compare the client's `expectedVersion` with the
stored value. A stale request returns 409. Hibernate versioning also protects
entity writes, while row locks serialize operations that share a shipment or
trip. The concurrent status test verifies the locking/version-check combination,
not an isolated optimistic-lock race.

## HTTP API

| Method | Route | Operation |
|---|---|---|
| GET / POST | `/api/customers` | List / create customers |
| GET / PUT / DELETE | `/api/customers/{id}` | Read / update / delete a customer |
| POST | `/api/shipments` | Create a shipment and its initial history event |
| GET | `/api/shipments/{id}` | Read a shipment |
| PUT | `/api/shipments/{id}/trip` | Assign or change a trip |
| PATCH | `/api/shipments/{id}/status` | Change status, except loading |
| GET | `/api/shipments/{id}/history` | Read internal history |
| GET | `/api/shipments/{id}/qr-code` | Generate a PNG containing the public tracking URL |
| GET | `/api/shipments/overdue` | List overdue undelivered shipments |
| GET | `/api/tracking/{trackingNumber}` | Read limited public tracking data |
| GET / POST | `/api/trips` | List / create trips |
| GET / PUT / DELETE | `/api/trips/{id}` | Read / update / deactivate a trip |
| PUT / DELETE | `/api/trips/{id}/employees/{employeeId}` | Add / remove a crew member |
| GET | `/api/trips/{id}/shipments` | List a trip's shipment assignment summaries |

Creation returns 201 with a `Location` header. Deletion/deactivation and crew
removal return 204. Request validation returns 400, missing resources return 404,
and explicit business/version conflicts return 409.

The overdue endpoint accepts zero-based `page` and `size` from 1 to 100, with
defaults of 0 and 20. Its JPQL query uses `expectedDeliveryAt < now`, excludes
`DELIVERED`, and orders by deadline and ID. Other list endpoints are unpaginated.

Public tracking includes the tracking number, status, creation time, expected
delivery time, and status/timestamp history. It excludes customer contact details,
addresses, and employee details. QR images are generated on demand; the base URL
is configured through `app.public-tracking-base-url`.

## Persistence and testing

Normal startup imports local connection settings from `.env` and uses
`ddl-auto=validate`. Database setup is currently manual SQL, not Flyway. The
scripts and their dependency order are documented in the backend README.
`open-in-view` is disabled; DTO mapping happens within service transactions.

Tests use an in-memory H2 database in PostgreSQL mode by default. A datasource
override can run integration tests against PostgreSQL. The concurrency and
creation test classes also create their own temporary schemas.

The suite covers customer and trip CRUD, crew membership, shipment creation and
history atomicity, assignment/capacity, loading/rollback, overdue queries, allowed
and forbidden transitions, and concurrent status updates. H2 execution is not a
substitute for checking PostgreSQL-specific locking and SQL behavior.

## Remaining work

- Expose loading over HTTP and enforce the active-crew rule before transit.
- Decide whether a separate delivery timestamp is needed in addition to history.
- Define customer deactivation and deletion behavior for customers with shipments.
- Add consistent API error bodies and database-conflict mapping.
- Add employee/vehicle management, shipment search, and pagination for other lists.
- Introduce versioned database migrations and dedicated public-tracking/QR tests.
- Add authentication and derive event actors from authenticated accounts. At
  present, callers supply an active employee ID, and account ownership is not enforced.
