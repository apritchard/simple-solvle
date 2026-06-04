# Testing And Verification

## Backend
Run from the repository root:

```powershell
mvn test -q
```

Backend tests cover solver behavior, word restrictions, dictionary loading, scoring, DTO/model behavior, job state, and Spring context startup. The Maven test phase also generates a JaCoCo coverage report at `target/site/jacoco/index.html`. Add or update tests when changing filtering, scoring, hard-mode, dictionary, or API-contract behavior.

The project targets Java 18 source compatibility and uses Java 21 for local and CI backend validation.

### Backend Fixture Notes
When tests need stable ordering for `Word` or `WordFrequencyScore` sets, build word sets through `Dictionary` or assign unique `Word` orders explicitly. Raw `new Word("...")` instances default to order `0`, and `WordFrequencyScore` equality/hash code use `naturalOrdering`, so multiple score objects with the same ordering can collapse unexpectedly inside sets.

## Frontend
Run from `solvle-front/`:

```powershell
npm.cmd test -- --watchAll=false --passWithNoTests
npm.cmd run build
```

The frontend has a smoke/render test for initial app load. Keep `--passWithNoTests` for compatibility with Create React App test discovery, but add focused tests for meaningful UI behavior changes.

## CI Expectations
GitHub Actions should run:

- Backend `mvn test` on Java 21.
- Frontend install, test, and production build.

## Before Finishing Work
- Run the smallest relevant checks for the files changed.
- For backend solver changes, run all backend tests.
- For frontend behavior changes, run frontend test and build, then use a browser smoke check when practical.
- Confirm `git status --short --branch` does not show accidental staged changes to dictionaries, backup data, keys, logs, or build outputs. Local backup data may appear as untracked and should remain untracked.
