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
- `src/components/layout/`: shared AppLayout, Sidebar, Header, and SVG navigation icons.
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
All routes share the Sidebar/Header/layout. Feature CRUD implementations remain separate tasks.

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
## Shared application layout

AppRoutes nests every route under AppLayout with an Outlet. The layout owns the
single main landmark, persistent desktop sidebar, header breadcrumbs, and footer.
Sidebar navigation uses NavLink and remains active on nested feature routes.
Header breadcrumbs and the browser title follow the current path and record ID.

Below 768px the sidebar becomes a modal drawer. The menu button reports its expanded
state; the drawer supports a close button, backdrop, Escape, focus trapping, focus
restoration, scroll locking, and closing on route selection. Resizing to desktop
restores normal navigation. A skip link leads to the main content.

CreateShipmentPage now contains its feature content only; customer fetching and
shipment submission retain their existing service integrations. The duplicate
sidebar/header/footer and their old CSS were removed. No placeholder health,
authentication, notification, or secure-connection claims are displayed by the shell.

Verification: 14 Jest/React Testing Library tests and the production build passed.
The actual UI was also inspected in headless Edge at desktop and narrow widths.
Layout tests cover nested active navigation, breadcrumbs, mobile focus/keyboard
behavior, route selection, and resizing. A form regression test uses mocked API
services to verify shipment submission inside the shared shell.
## Option 2 design tokens

src/styles/tokens.css owns the dark navy/mint palette, semantic status colors,
system font stack with Cyrillic support, type scale, spacing, radii, shadows,
focus treatment, and motion duration. Component CSS Modules consume these tokens.
Use 16px inputs/body text, 14px labels, 12px captions, and 24?32px page headings.
The shared shell, preparation pages, and shipment creation form follow this scale.
No external font or styling dependency is needed.

## Shared UI

Button, Modal, FormField, and DataTable live in src/components/ui/ and use
the shared design tokens. See [their contracts and examples](src/components/ui/README.md).
Shipment creation already uses Button and FormField. Modal and DataTable are
available for the upcoming CRUD screens without adding placeholder API actions.
