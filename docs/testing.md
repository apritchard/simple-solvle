# Testing And Verification

## Backend
Run from the repository root:

```powershell
mvn test -q
```

Backend tests cover solver behavior, word restrictions, dictionary loading, scoring, DTO/model behavior, job state, and Spring context startup. The Maven test phase also generates a JaCoCo coverage report at `target/site/jacoco/index.html`. Add or update tests when changing filtering, scoring, hard-mode, dictionary, or API-contract behavior.

### Benchmark And Exploration Suites

Two slow JUnit groups are excluded from the default `mvn test` run via JUnit `@Tag`:

- `@Tag("benchmark")` — `DictionaryBenchmarkTest`. Regression tests for solver quality on the full SIMPLE dictionary, comparing against committed baselines under `src/test/resources/benchmarks/`. Each test method runs one `WordConfig` through 1 warm-up + 3 measurement runs of `solveDictionary` and asserts against tolerance-controlled deltas in mean, max, failure count, and distribution shift. Runtime is reported as `[BENCHMARK WARN]` if it regresses by more than 25% but never fails the test.
- `@Tag("exploration")` — `DictionaryExplorationTest`. The manual playground around the full-dictionary solver: starter-word search, parameter-grid sweeps, and one-shot inspection of solver/analysis output. Asserts nothing; reads through the logs to interpret the results.

Run via Maven profiles:

```powershell
mvn -Pbenchmark test                                       # run benchmark suite, fail on solve-quality regression
mvn -Pbenchmark test -Dbenchmark.baseline.write=true       # overwrite committed baselines with current results
mvn -Pexploration test                                     # run the exploration suite (no assertions)
mvn -Pexploration test -Dtest=DictionaryExplorationTest$StarterWordSearch   # run just one sub-bucket
```

`mvn -Pbenchmark test` takes ~15–20 minutes on a developer laptop (6 shipped configs × 4 runs each over 2,315 SIMPLE solutions). The exploration suite is unbounded — the parameter grids are intentionally large; run individual `@Nested` classes or methods rather than the whole thing.

When a benchmark regression is intentional, regenerate the baseline with `-Dbenchmark.baseline.write=true` and commit the updated JSON file. The git diff records what changed; the commit message should record why.

Current reports for the latest local run land in `target/benchmark-reports/<config>-<dictionary>.json`. These are git-ignored; only the `*-baseline.json` files under `src/test/resources/benchmarks/` are committed.

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
`.github/workflows/ci.yml` is authoritative. It runs backend `mvn test` and frontend install, test, and production build on every push and pull request.

## Before Finishing Work
- Run the smallest relevant checks for the files changed.
- For backend solver changes, run all backend tests.
- For frontend behavior changes, run frontend test and build, then use a browser smoke check when practical.
- Confirm `git status --short --branch` does not show accidental staged changes to dictionaries, backup data, keys, logs, or build outputs. Local backup data may appear as untracked and should remain untracked.
