# Solvle Agent Guide

## Project Overview
Solvle is a word-puzzle analysis app. The backend is a Spring Boot service that evaluates word restrictions and solver strategies. The frontend is a Create React App UI that sends puzzle state to the backend and renders suggested guesses, fishing words, tuple scores, and game ratings.

## Repository Layout
- `src/main/java/com/appsoil/solvle/` - Spring Boot backend.
- `src/main/resources/dict2/` - bundled dictionaries and solution lists.
- `src/test/java/com/appsoil/solvle/` - backend tests.
- `solvle-front/` - React frontend.
- `docker-compose.yaml` - local two-service Docker setup.
- `aws-backup/` - local historical AWS backup data if present. Treat as sensitive and do not stage.

## Local Commands
Backend:

```powershell
mvn test -q
```

Frontend from `solvle-front/`:

```powershell
npm.cmd test -- --watchAll=false --passWithNoTests
npm.cmd run build
```

Use `npm.cmd` in PowerShell if script execution policy blocks `npm.ps1`. On shells without that restriction, `npm` is fine.

Run locally:

```powershell
mvn spring-boot:run
cd solvle-front
npm.cmd start
```

The backend listens on port `8081`. The frontend dev server listens on port `3000` and proxies `/solvle` and `/solvescape` to the backend.

Docker:

```powershell
docker-compose up
```

Docker exposes the backend on `8081` and the frontend on `80`.

## Runtime Notes
- Java source level is `18`.
- Java 21 is the standard local and CI backend validation runtime.
- Lombok is pinned for Java 21 compiler compatibility.
- The frontend Dockerfile and CI use Node 17. Avoid runtime modernization unless the task explicitly asks for it.

## Domain Guardrails
- Dictionary files in `src/main/resources/dict2/` are domain data. Do not edit them casually or reformat them as part of unrelated work.
- Preserve Wordle-style tile semantics: green means exact position, yellow means present in another position, gray means unavailable subject to duplicate-letter rules.
- Keep solver scoring, filtering, hard-mode, and `requireAnswer` behavior covered by tests when changing backend logic.
- Fishing words may be non-solutions used for information gain. Do not collapse them into answer-only lists unless the task explicitly changes that behavior.

## API Guardrails
- Backend routes are rooted at `/solvle`.
- The React app uses relative fetches such as `/solvle/...`; keep frontend and backend API contracts aligned.
- Avoid DTO shape changes without updating both frontend consumers and backend tests.

## Infrastructure Guardrails
Solvle was previously hosted on AWS, but hosting is inactive. A local `aws-backup/` directory may contain historical EC2, VPC, load-balancer, DNS, and security-group metadata. Use it only as sensitive historical context. Do not commit it, quote account/resource identifiers, or derive new infrastructure-as-code from it during ordinary app work.

## Git And Staging
- Always inspect `git status --short --branch` before editing and before staging.
- Preserve unrelated user work in the working tree.
- For the AI onboarding work, stage only onboarding docs, CI files, and the Lombok compatibility change unless the user explicitly expands scope.
- Do not stage `aws-backup/`, private keys, logs, build outputs, or unrelated solver experiments.
