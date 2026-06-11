# Solvle Agent Guide

## Project Overview
Solvle is a word-puzzle analysis app. The backend is a Spring Boot service that evaluates word restrictions and solver strategies. The frontend is a Create React App UI that sends puzzle state to the backend and renders suggested guesses, fishing words, tuple scores, and game ratings.

## Repository Layout
- `src/main/java/com/appsoil/solvle/` - Spring Boot backend.
- `src/main/resources/dict2/` - bundled dictionaries and solution lists.
- `src/test/java/com/appsoil/solvle/` - backend tests.
- `solvle-front/` - React frontend.
- `docker-compose.yaml` - builds and runs the single combined container locally.
- `docs/` - architecture, API contracts, dev setup, testing, coverage spec, deploy.
- `aws-backup/` - local historical AWS backup data if present. Treat as sensitive and do not stage.

## Workflow
1. Check `git status --short --branch` before editing and before staging.
2. Read the relevant backend or frontend files before editing; preserve unrelated user work.
3. Identify the smallest verification command that proves the change.
4. Prefer existing patterns over new abstractions.
5. Check `docs/api-contracts.md` before changing restriction strings, DTOs, or `/solvle` fetch calls.

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

Run locally, then verify backend-backed interactions at `http://localhost:3000`:

```powershell
mvn spring-boot:run
cd solvle-front
npm.cmd start
```

The backend listens on port `8081`. The frontend dev server listens on port `3000` and proxies `/solvle` and `/solvescape` to the backend.

Docker (`docker-compose up`) builds and runs the single combined container on `http://localhost:8081` — the same image that ships to production. Details in `docs/dev-setup.md`.

Slow benchmark/exploration suites and the coverage workflow are documented in `docs/testing.md`.

## Toolchain
- Authoritative versions live in the build files: `pom.xml` for Java and Spring Boot, the root `Dockerfile` and `.github/workflows/ci.yml` for Node. Summary in `docs/dev-setup.md`.
- Avoid runtime modernization unless the task explicitly asks for it.

## Domain Guardrails
- Dictionary files in `src/main/resources/dict2/` are domain data. Do not edit them casually or reformat them as part of unrelated work.
- Distinguish solution dictionaries from valid-guess dictionaries. `getPrimarySet(wordList)` is the possible answer set; `getFishingSet(wordList)` is the valid guess/information-gain set.
- For English solution lists such as `SIMPLE`, `EXTENDED`, and `REDUCED`, valid fishing/guess words come from `DictionaryType.BIG` (`enable1.txt`) and may not be valid answers.
- Preserve Wordle-style tile semantics: green means exact position, yellow means present in another position, gray means unavailable subject to duplicate-letter rules.
- Keep solver scoring, filtering, hard-mode, and `requireAnswer` behavior covered by tests when changing backend logic.
- Fishing words may be non-solutions used for information gain. Do not collapse them into answer-only lists unless the task explicitly changes that behavior.

## API Guardrails
- Backend routes are rooted at `/solvle`.
- The React app uses relative fetches such as `/solvle/...`; keep frontend and backend API contracts aligned.
- Avoid DTO shape changes without updating both frontend consumers and backend tests.

## Infrastructure Guardrails
- **Every push to `main` deploys to production.** `.github/workflows/deploy.yml` builds the combined container and rolls it out to an AWS Lightsail container service (live at https://solvle.appsoil.com). Treat merging a PR to `main` as a production deploy and say so when proposing a merge. See `docs/deploy.md` for the deployment setup.
- A local `aws-backup/` directory may contain historical metadata from the prior EC2-based hosting (VPC, load-balancer, DNS, security groups). Use it only as sensitive historical context. Do not commit it, quote account/resource identifiers, or derive new infrastructure-as-code from it during ordinary app work.

## Git And Staging
- Do not stage `aws-backup/`, private keys, logs, build outputs, or unrelated solver experiments.
- Add or update tests for solver, restriction-string, scoring, API, or UI behavior changes.
- Document frontend/backend API contract changes in the PR.
