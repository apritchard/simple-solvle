# Testing And Verification

## Backend
Run from the repository root:

```powershell
mvn test -q
```

Backend tests cover solver behavior, word restrictions, dictionary loading, scoring, and Spring context startup. Add or update tests when changing filtering, scoring, hard-mode, dictionary, or API-contract behavior.

The project targets Java 18 source compatibility and uses Java 21 for local and CI backend validation.

## Frontend
Run from `solvle-front/`:

```powershell
npm.cmd test -- --watchAll=false --passWithNoTests
npm.cmd run build
```

The current app has no frontend tests, so the test command exits successfully with `--passWithNoTests`. The production build currently succeeds with lint warnings; do not treat those warnings as part of unrelated changes unless the task asks for frontend cleanup.

## CI Expectations
GitHub Actions should run:

- Backend `mvn test` on Java 21.
- Frontend install, test, and production build.

## Before Finishing Work
- Run the smallest relevant checks for the files changed.
- For backend solver changes, run all backend tests.
- For frontend behavior changes, run frontend test and build, then use a browser smoke check when practical.
- Confirm `git status --short --branch` does not show accidental staged changes to dictionaries, backup data, keys, logs, or build outputs. Local backup data may appear as untracked and should remain untracked.
