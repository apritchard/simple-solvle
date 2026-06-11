# Contributing

## Start Here
Solvle is a Spring Boot backend with a Create React App frontend. `AGENTS.md` is the working guide — commands, workflow, and guardrails — and `docs/` holds architecture, API contracts, dev setup, testing, and deploy details.

## Verification
Run the commands in `AGENTS.md` Local Commands (backend `mvn test`, frontend test + build). The full testing and coverage workflow is in `docs/testing.md`.

## Pull Requests
- Merging to `main` triggers a production deploy (see `docs/deploy.md`).
- Keep unrelated local work out of the commit.
- Document frontend/backend API contract changes.
- Add or update tests for solver, restriction-string, scoring, API, or UI behavior changes.
- Do not stage `aws-backup/`, private keys, logs, or build output.
