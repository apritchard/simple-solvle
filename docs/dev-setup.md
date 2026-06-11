# Development Setup

## Prerequisites
- Java 21 — authoritative version is `java.version` in `pom.xml` (currently also the Spring Boot 3.5 baseline).
- Maven 3.8 or newer.
- Node 20 — authoritative version is the root `Dockerfile` base image and `.github/workflows/ci.yml`.
- Docker Desktop or another Docker Compose provider if using containerized startup.

## Backend
From the repository root:

```powershell
mvn test -q
mvn spring-boot:run
```

The backend listens on port `8081`, configured in `src/main/resources/application.properties`.

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

Docker Compose builds the single combined image from the root `Dockerfile`: the React production build is bundled into the Spring Boot jar as static resources, and one container serves both UI and API on `8081`. Open `http://localhost:8081`. This is the same image that deploys to production.

## Hosting
The app is live on an AWS Lightsail container service at https://solvle.appsoil.com, deployed automatically on every push to `main`. See `docs/deploy.md` for the full setup and `AGENTS.md` for deploy and backup-data guardrails.
