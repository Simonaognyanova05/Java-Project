# Smart Shipment Tracking — Frontend Guidelines

Status: agreed baseline for development.
Selected design: **Option 2 — Dark Dispatch Center**.
Stack: **React with Create React App**, JavaScript and JSX, npm.

This document is the shared reference for everyone working on the frontend.
Update it in the same pull request whenever an agreed decision changes.
Features must follow the actual Spring Boot DTOs and endpoints.
The documentation is in English; application labels remain in Bulgarian.

## 1. Visual Reference

Primary mockup: [Option 2](frontend-concepts/02_dark_dispatch/dashboard.png).
Concept description: [Dark Dispatch Center](frontend-concepts/02_dark_dispatch/README.md).

![Selected frontend design](frontend-concepts/02_dark_dispatch/dashboard.png)

Follow this option's composition, color scheme, and visual hierarchy.
The image contains sample data; it does not define the API contract or business rules.

- Dark backgrounds, distinct panels, and light text.
- Mint/turquoise accents for primary actions and active elements.
- Orange for warnings and delays; red for errors.
- Left navigation, top bar, summary cards, route visualization, trip details, and a shipment table.
- Short Bulgarian UI labels; English filenames and identifiers.
- Restrained decoration that keeps data and actions easy to read.

## 2. Project Creation and Startup

The frontend belongs in a root-level `frontend/` directory, alongside `smart-shipment-tracking/`.
Create the application once, from the repository root:

```powershell
npx create-react-app@latest frontend
cd frontend
npm start
```

The command is `npx create-react-app`, with a space after `npx`.
Do not run the generator again for an existing project. After checking out the code:

```powershell
cd frontend
npm ci
npm start
```

Development addresses: frontend `http://localhost:3000`, backend `http://localhost:8080`.
Start the backend in a separate terminal from `smart-shipment-tracking/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Create React App is deprecated according to the official React documentation.
It remains the explicitly selected tool for this project. Discuss build-tool
changes with the team instead of introducing them independently in a feature branch.

During initial setup, record the supported Node version in `.nvmrc` or the frontend README.
Commit `package-lock.json` and use npm consistently. Dependency versions are captured
in the lockfile; do not mix npm and Yarn.

## 3. Technology Decisions

| Area | Shared decision |
|---|---|
| UI | React functional components and hooks |
| Language | JavaScript and JSX |
| Routing | React Router with a shared route configuration |
| Styling | CSS variables and component CSS Modules |
| HTTP | One shared client built on `fetch` |
| Local state | `useState` and `useReducer`, depending on complexity |
| Shared state | Context only for state genuinely shared across screens |
| Icons | One consistent set of outline SVG icons |
| Testing | Jest and React Testing Library from the CRA setup |

Add dependencies only for a concrete need and explain them in the PR.
Use the shared HTTP client and UI conventions across features.

## 4. Frontend Structure

```text
frontend/
  public/
  src/
    app/
      App.js
      AppRoutes.js
    components/
      layout/
        AppLayout.jsx
        Sidebar.jsx
        Header.jsx
      ui/
        Button.jsx
        Modal.jsx
        StatusBadge.jsx
        DataTable.jsx
        FormField.jsx
        EmptyState.jsx
        ErrorState.jsx
        LoadingState.jsx
    features/
      dashboard/
      shipments/
      trips/
      customers/
      employees/
      vehicles/
    services/
      apiClient.js
      shipmentApi.js
      tripApi.js
      customerApi.js
    hooks/
    constants/
      shipmentStatuses.js
    utils/
      dateFormat.js
      numberFormat.js
    styles/
      tokens.css
      global.css
    assets/
    index.js
    setupTests.js
  .env.example
  .gitignore
  package.json
  package-lock.json
```

Keep CRA's `src/index.js` entry point; it imports `app/App.js`.
Each feature directory contains its page, feature-specific components, and a feature
hook when needed. Move components into `components/` once they have more than one
real use.

## 5. Colors, Typography, and Spacing

Initial shared tokens for `src/styles/tokens.css`:

```css
:root {
  --color-bg: #08131d;
  --color-surface: #101f2b;
  --color-surface-hover: #162b39;
  --color-border: #243b4b;
  --color-text: #f3f6fa;
  --color-text-muted: #a5b8ca;
  --color-primary: #36e6bc;
  --color-on-primary: #06231c;
  --color-warning: #ffab63;
  --color-danger: #ff7785;
  --color-info: #67b8ff;

  --font-body: "Inter", "Segoe UI", sans-serif;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-6: 24px;
  --space-8: 32px;
  --sidebar-width: 208px;
}
```

Use 16px body text, 14px table and secondary text, and 24–32px headings.
If Inter is included, use a version with Cyrillic support and follow its license.
All components use the shared tokens. Adjust colors and dimensions centrally
rather than introducing a separate palette for each page.

## 6. Layout and Responsive Behavior

- Wide screens: persistent sidebar, header, and main workspace.
- Dashboard: summary cards, central route, selected-trip panel, and lower table.
- Shipment details open in a side panel or dedicated page, depending on the screen.
- Below 1200px, rearrange panels into fewer columns.
- Below 768px, replace the sidebar with a menu button and stack panels vertically.
- Tables may scroll horizontally inside their container without widening the page.
- Forms and dialogs remain usable with a keyboard and on narrow screens.

Use semantic interactive elements such as `button`, `a`, and `input`.
Provide visible focus, field labels, dialog focus management, and accessible names
for icon buttons. Status indicators include text as well as color.

## 7. Screens and Routes

| Route | Screen | Main purpose |
|---|---|---|
| `/` | Redirect to `/dashboard` | Consistent entry point |
| `/dashboard` | Overview | Summaries, selected trip, available shipments, and delays |
| `/shipments` | Shipment workspace | Available reports and lookup by ID |
| `/shipments/new` | New shipment | Creation form |
| `/shipments/:id` | Shipment details | Data, status, and trip assignment |
| `/trips` | Trips | Listing, creation, and editing |
| `/trips/:id` | Trip details | Vehicle, crew, and assigned shipments |
| `/customers` | Customers | Existing Customer CRUD |
| `/employees` | Crew | Planned management after the API is available |
| `/vehicles` | Fleet | Planned management after the API is available |
| `*` | Page not found | Navigation back to the application |

A general shipment list, tracking-number search, and employee/vehicle management
require additional backend endpoints. Until those exist, expose supported
operations or a clearly labeled demonstration view. Navigation for planned modules
must not lead to forms that appear to save data when no operation exists.

## 8. Available API Contract

This table reflects the current merged implementation. Java Controller and DTO
classes are the source of truth for exact request and response fields.

| Method and endpoint | Purpose |
|---|---|
| `GET /api/customers` | List customers |
| `POST /api/customers` | Create a customer |
| `GET /api/customers/{id}` | Read a customer |
| `PUT /api/customers/{id}` | Update a customer |
| `DELETE /api/customers/{id}` | Hard-delete a customer in the current implementation |
| `GET /api/trips` | List active trips |
| `POST /api/trips` | Create a trip |
| `GET /api/trips/{id}` | Read an active trip |
| `PUT /api/trips/{id}` | Update a trip |
| `DELETE /api/trips/{id}` | Deactivate a trip |
| `PUT /api/trips/{id}/employees/{employeeId}` | Add an active employee to the crew |
| `DELETE /api/trips/{id}/employees/{employeeId}` | Remove an employee from the crew |
| `POST /api/shipments` | Create a shipment |
| `GET /api/shipments/{id}` | Read shipment details |
| `PUT /api/shipments/{id}/trip` | Primary assignment endpoint |
| `GET /api/trips/{id}/shipments` | Limited projection of shipments assigned to a trip |
| `GET /api/shipments/trip/{tripId}` | Detailed staff report for a trip's shipments |
| `GET /api/shipments/overdue?page=0&size=20` | Overdue shipments; size from 1 to 100 |

Use the primary assignment endpoint in the new frontend. The legacy route
`PUT /api/shipments/{shipmentId}/trip/{tripId}?expectedVersion=...` remains for
compatibility but is not the preferred frontend contract.

HTTP endpoints for a general shipment list, status changes, history, public tracking
lookup, QR images, employee/vehicle CRUD, and dashboard statistics are not available
yet. `ShipmentLoadingService` is a service operation, not an HTTP endpoint.
Do not simulate successful operations in place of a missing API.

### Example: Create a Shipment

```json
{
  "customerId": 1,
  "recipientName": "Maria Petrova",
  "deliveryAddress": "Vienna 1",
  "weightKg": 10.5,
  "expectedDeliveryAt": "2030-01-01T12:00:00Z"
}
```

The server generates `trackingNumber`; the form does not send it.
POST returns `201`, with an ID, tracking number, status, and version in the response.

### Example: Assign a Shipment to a Trip

```text
PUT /api/shipments/12/trip
```

```json
{
  "tripId": 3,
  "expectedVersion": 0
}
```

Take the version from the latest shipment response; never hardcode it to 0.

### Trip Fields

`TripRequest`: `origin`, `destination`, `departureAt`, `expectedArrivalAt`, `vehicleId`.
`TripResponse` also exposes `id`, `version`, `deleted`, and `employeeIds`.
Do not introduce a separate TripStatus or trip tracking number based on the mockup.
Arrival must follow departure. A legacy trip may have no vehicle; new create/update
requests require `vehicleId`.

## 9. HTTP Client and Error Handling

Route every request through `services/apiClient.js`. Components do not construct
their own backend URLs. The client:

- Sets `Accept: application/json` and adds `Content-Type` when sending JSON.
- Checks `response.ok`; `fetch` does not throw automatically for HTTP 4xx/5xx.
- Handles `204` without calling `response.json()`.
- Supports cancellation with `AbortController` when the screen or request changes.
- Normalizes errors into a shared structure containing status and message,
  including non-JSON responses. Do not assume every backend error is ProblemDetail.

| Situation | UI behavior |
|---|---|
| `400` | Invalid-input message; field errors when supplied by the API |
| `404` | Record not found or no longer active |
| `409` | Conflict explanation and an option to reload current data |
| `5xx` or network failure | Clear message and manual retry |
| Future `401/403` | Handle when actual authentication is introduced |

On `409`, do not automatically retry a mutating request or replace the current
version with a client-generated value. Preserve form input and allow review.
Disable the submit button while a request is pending to prevent double submission.

## 10. Configuration and .env

Backend secrets remain in the ignored `smart-shipment-tracking/.env` file.
Never expose database passwords, private API keys, or signing secrets to the frontend.
CRA embeds `REACT_APP_*` values in the public JavaScript bundle: frontend `.env`
files provide public configuration, not secret storage.

Example `frontend/.env.example`:

```dotenv
PORT=3000
REACT_APP_API_BASE_URL=/api
```

Local values may live in `frontend/.env.local`. During frontend setup, ignore
`.env` and `.env.*` in its `.gitignore`, with `!.env.example` as an exception.
Restart the CRA development server after changing environment variables.

For development, add this field to `frontend/package.json`, preserving its other fields:

```json
"proxy": "http://localhost:8080"
```

The API client uses `/api` as its base; individual services pass paths such as
`/trips`, producing `/api/trips`. Avoid `/api/api` and component-specific absolute
URLs that bypass the proxy. This backend address is public local configuration.

The CRA proxy applies only to `npm start`. Deployment requires an `/api` reverse
proxy or an agreed separate backend origin and CORS configuration.

## 11. Statuses, Dates, Weight, and Capacity

Keep the shared status mapping in `constants/shipmentStatuses.js`:

| Backend value | Bulgarian UI label | Visual treatment |
|---|---|---|
| `CREATED` | Създадена | Neutral |
| `ACCEPTED` | Приета | Blue |
| `LOADED` | Натоварена | Purple |
| `IN_TRANSIT` | В транспорт | Turquoise |
| `DELIVERED` | Доставена | Green |

The sequence remains `CREATED → ACCEPTED → LOADED → IN_TRANSIT → DELIVERED`.
An overdue condition is derived from the deadline; it is not a new enum status.
The backend enforces business transitions and capacity limits.

- API timestamps use ISO 8601; respect the actual offset.
- Convert selected local date/time values to UTC ISO before submitting forms.
- Display dates through a shared `bg-BG` formatter and an explicit user time zone.
- Do not hardcode UTC+2/UTC+3 or compare formatted date strings.
- Display weight and capacity in kg; do not automatically treat missing values as 0.
- Capacity usage is the sum of all assigned shipment weights divided by capacityKg.
- When capacityKg is unavailable, show the Bulgarian label “Няма данни” (No data)
  instead of inventing a limit or counting only shipments with status LOADED.

## 12. Map, Metrics, and Demonstration Data

The initial map may be a schematic visualization of the selected route. A real
geographic map requires a separate choice of library, tile provider, and applicable
permissions/attribution. Live GPS tracking requires a corresponding API.

Dashboard values come from real reports. When an endpoint is unavailable, display
“Няма данни” (No data) or a clearly labeled demo mode. Do not treat mockup numbers
as actual statistics or derive a global count from one page of overdue results.

Keep sample data separate from API services. Do not automatically substitute it
after a network failure. Names, dates, QR codes, percentages, and route numbers in
the mockup are illustrative; their appearance does not imply an implemented backend.

## 13. Coding and Collaboration Rules

- Components/pages: PascalCase; hooks: `use...`; functions/variables: camelCase.
- CSS Module: `ComponentName.module.css`, next to its component.
- Reuse tables, status colors, dialogs, and HTTP logic across modules.
- Pages coordinate data and actions; shared UI components do not know the API.
- Every screen handles loading, empty, error, and success states.
- Distinguish an empty result from a failed request.
- React accesses data through the Spring REST API, never directly through PostgreSQL.
- Authentication is a separate contract; do not implement login UI with pretend protection.
- Coordinate changes to shared tokens, layout, routing, the API client, and dependencies.

Use separate feature branches. A PR describes the screen, endpoints, and validation
performed, and includes screenshots for visual changes. Identify missing backend
dependencies. Coordinate SQL and DTO changes between frontend and backend, updating
this document in the same PR where necessary.

## 14. Implementation Order

1. CRA project, shared tokens, layout, router, and API client.
2. Customer CRUD as the first complete React-to-backend integration.
3. Trips: listing, forms, details, and existing crew actions.
4. Shipment creation/lookup and trip assignment using expectedVersion.
5. Overdue shipments with actual pagination.
6. Dashboard based on available data and a schematic route.
7. Extend backend contracts for general listing, tracking, history, status changes,
   employees, vehicles, and statistics; then implement the corresponding UI.
8. Review responsive behavior, accessibility, and visual consistency with Option 2.

## 15. Definition of Done

- Follows the selected dark design and uses shared components/tokens.
- Uses the agreed endpoint and exact DTO fields.
- Includes appropriate loading/empty/error states and form validation.
- Handles `409` where business or version conflicts can occur.
- Uses semantic controls and works with a keyboard and on narrow screens.
- Introduces no console errors, React key warnings, or broken requests.
- `npm run build` succeeds, and meaningful tests for changed behavior pass:

```powershell
npm test -- --watchAll=false
npm run build
```

Tests cover user behavior such as submission, validation, empty reports, and
responses to 409 conflicts. Tests are not required for every static decoration.

## 16. Official Setup References

- [Create React App — Getting Started](https://create-react-app.dev/docs/getting-started/)
- [React — Sunsetting Create React App](https://react.dev/blog/2025/02/14/sunsetting-create-react-app)
- [CRA — Environment Variables](https://create-react-app.dev/docs/adding-custom-environment-variables/)
- [CRA — Development Proxy](https://create-react-app.dev/docs/proxying-api-requests-in-development/)

The CRA application now exists in frontend/. See frontend/README.md for the current
implementation status and development commands.

## 17. CRA Foundation and Routing Status

The existing CRA application and installed React Router dependency are reused.
src/index.js mounts src/app/App.js, which owns BrowserRouter and global styles.
AppRoutes.js centralizes the routes listed in section 7, including the root redirect
to /dashboard, dynamic shipment/trip IDs, and a not-found recovery page.

The existing /shipments/new form and its service integrations are preserved.
Other feature routes currently render explicitly labeled preparation screens;
they do not represent completed CRUD or make simulated backend requests. The
permanent application layout, Sidebar, Header, and subsequent API-backed screens
remain separate tasks. Temporary preparation screens share RoutePlaceholder.

Feature directories and shared extension points follow section 4. src/App.js is
a compatibility re-export; new application configuration belongs in src/app/.
Router behavior is tested with MemoryRouter and does not require a running backend.