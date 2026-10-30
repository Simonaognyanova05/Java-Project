# Smart Shipment Tracking frontend

React SPA built with Create React App, JavaScript/JSX, and React Router.
Shared decisions are documented in [FRONTEND_GUIDELINES.md](../FRONTEND_GUIDELINES.md).

## Development

```powershell
npm ci
npm start
```

The development server runs at http://localhost:3000. The existing package.json
proxy forwards API requests to Spring Boot at http://localhost:8080; start the
backend separately to use shipment creation. This task does not change backend
contracts or add backend dependencies to placeholder routes.

## Structure

- `src/index.js`: CRA entry point; mounts `app/App.js`.
- `src/app/App.js`: BrowserRouter and global style imports.
- `src/app/AppRoutes.js`: central route definitions.
- `src/app/navigation.js`: shared navigation labels and paths.
- `src/features/`: feature pages, including the existing shipment creation screen.
- `src/components/ui/`: shared presentation components.
- `src/components/layout/`: reserved for the next layout task.
- `src/services/`: existing backend integration modules.
- `src/styles/`: existing design tokens and global CSS.
- `src/hooks/`, `src/constants/`, `src/utils/`, `src/assets/`: shared extension points.
- `src/App.js`: compatibility re-export of `app/App.js`.

## Routes

| URL | Current behavior |
|---|---|
| `/` | Redirects to `/dashboard` |
| `/dashboard` | Overview placeholder |
| `/shipments` | Shipment workspace placeholder |
| `/shipments/new` | Existing shipment creation form |
| `/shipments/:id` | Shipment details placeholder |
| `/trips` | Trip list placeholder |
| `/trips/:id` | Trip details placeholder |
| `/customers` | Customer management placeholder |
| `/employees` | Crew management placeholder |
| `/vehicles` | Fleet management placeholder |
| Other paths | Not-found page with a recovery link |

Placeholders are clearly labeled; they do not fetch or simulate business data.
The permanent Sidebar/Header/layout and feature CRUD implementations are separate tasks.

## Verification

```powershell
npm test -- --watchAll=false --runInBand
npm run build
```

Router tests cover the root redirect, navigation, direct URLs, dynamic IDs, and
not-found recovery. Shipment creation is replaced by a test double only in these
router tests, so routing checks do not depend on a running backend.

## Hosting

For production, configure the static host to return index.html for client routes
such as /trips/17. Route /api requests to the backend before applying the SPA
fallback. CRA's package.json proxy applies only to npm start.

CRA uses an older Jest resolver. The package.json Jest moduleNameMapper resolves
React Router 7's react-router/dom export to its installed CommonJS entry point;
setupTests.js supplies the Encoding API for jsdom. These are test-environment
compatibility settings; the browser build uses the package's normal exports.