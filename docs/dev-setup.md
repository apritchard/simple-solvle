# Development Setup

## Prerequisites
- Java 21 for local backend validation.
- Maven 3.8 or newer.
- Node compatible with the existing React app. The Dockerfile and CI currently use Node 17.
- Docker Desktop or another Docker Compose provider if using containerized startup.

## Backend
From the repository root:

```powershell
mvn test -q
mvn spring-boot:run
```

The backend listens on port `8081`, configured in `src/main/resources/application.properties`.

The project source level remains Java 18. Lombok is pinned so Java 21 compilation works without changing the source target.

## Frontend
From `solvle-front/`:

```powershell
npm.cmd install
npm.cmd start
```

The React dev server listens on port `3000`. Its proxy forwards `/solvle` and `/solvescape` to `http://localhost:8081`.

PowerShell may block `npm.ps1` on some Windows machines. Use `npm.cmd` for local commands when that happens.

## Docker
From the repository root:

```powershell
docker-compose up
```

Docker Compose builds the backend from the root `Dockerfile` and the frontend from `solvle-front/Dockerfile`. The backend is exposed on `8081`; the frontend is exposed on `80`.

## Historical Hosting
The app was previously deployed on AWS, but hosting is inactive. Infrastructure modernization is a future task; see `AGENTS.md` for local backup handling guardrails.
