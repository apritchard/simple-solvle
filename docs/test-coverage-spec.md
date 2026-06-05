# Test Coverage Review And Spec

This document captures the current test coverage baseline and the test plan to complete before changing solver or UI behavior. Coverage percentages are useful for finding blind spots, but the target is behavioral confidence: restriction semantics, dictionary selection, scoring, API contracts, and the main UI workflows should each have direct tests.

## Baseline Commands

Backend tests:

```powershell
mvn test -q
```

Backend one-off coverage:

```powershell
mvn -q org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report
```

Frontend coverage:

```powershell
cd solvle-front
npm.cmd test -- --watchAll=false --coverage --passWithNoTests
```

Backend benchmark and exploration suites (opt-in, not part of the default coverage run):

```powershell
mvn -Pbenchmark test                                       # solve-quality regression vs. committed baselines
mvn -Pbenchmark test -Dbenchmark.baseline.write=true       # regenerate baselines
mvn -Pexploration test                                     # manual playground; asserts nothing
```

See [testing.md](testing.md) for the full benchmark workflow.

## Coverage Status

Backend validation runs 169 active tests (default `mvn test`). Product-code JaCoCo coverage:

| Metric | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 6,326 | 6,875 | 92.01% |
| Branches | 378 | 530 | 71.32% |
| Lines | 1,034 | 1,069 | 96.73% |

A `jacoco:check` execution in `pom.xml` enforces these minimum floors on every `mvn test`: instructions ≥85%, branches ≥65%, lines ≥90%. The experimental package (`com.appsoil.solvle.experimental`) is excluded from the report.

Trajectory across this work: started at 40.31% / 30.30% / 41.78% (instructions / branches / lines) with 74 tests. Chunks landed in this order — JaCoCo tooling + first backend chunk, MockMvc controller contracts, focused calculation-service tests, `SolvleService` orchestration, `RemainingSolver` solve loops, restriction-edge tests, legacy-code classification (`PreloadService` removed; `GroupSolver` + `SolvescapeService` moved to experimental and excluded), `SolvleService` solver-orchestration (playout, `solveDictionary` overloads, `submitTupleJob` cache/restart/completion), and the final targeted-gaps pass (positional `getScore`, partitioning branch, blank-firstWord bestWords path, German fishing routing, third `solveDictionary` overload, expanded EXTENDED fixture for the `submitTupleJob` map block, fixed harmonic series, rutBreak threshold skip path, hard-mode rut detection, playout failures, `KnownPosition.compareTo` branches, `SharedPositions.sortedPositionStream` ordering, and the tuple-job idle-timeout path via a settable seam).

Notes worth keeping:
- `WordCalculationService#harmonic` was broken — `1.0 / (double)n` instead of `1.0 / (double)i` produced ~1.0 for every input. Fixed during the targeted-gaps pass. A new `WordCalculationConfig.withHarmonic(boolean)` builder + `OPTIMAL_MEAN_WITH_PARTITIONING_HARMONIC` `WordConfig` and benchmark variant let us measure whether the corrected series helps (initial baseline: it slightly hurts vs. the flagship).
- The three rutBreak call sites in `SolvleService` were re-enabled. Guards (`rutBreakThreshold > 1`, `rutBreakMultiplier > 0`) keep all pre-existing configs no-op since they default to 0; new `OPTIMAL_MEAN_HARD_MODE` and `OPTIMAL_MEAN_HARD_MODE_RUTBREAK` configs exercise the path.
- The tuple-job idle-timeout path had a latent bug — `setStatus(JobStatus.COMPLETED)` at the end of `finishTuple` unconditionally overwrote `FAILED` from the in-flight timeout. Now wrapped in `if (!timeout.get())`. `maxJobIgnoreTimeSeconds` and `tupleJobTimeoutCheckInterval` are package-private fields with setters so the test can drive the path deterministically.
- Remaining intentional gaps: `SolvleService` defensive executor `catch` for a failed tuple job runnable, and the two priority-queue swap-on-better-score `else` branches in `generateNWordListsHeuristic` (need ~65+ word fixture to hit `TOP_N=2000`).

Backend class coverage signal:

| Area | Current signal |
| --- | --- |
| `WordRestrictions` | Strong line and branch coverage around parsing and generated restrictions. Direct edge coverage is now in place via `WordRestrictionsTest`: parsing (`g5^2!2` style position/frequency/exclusion), `generateRestrictions` duplicate-letter Wordle semantics, `withAdditionalLetterPositions` merges, and combined `isValidWord` position/frequency/exclusion checks. |
| `WordCalculationService` | First-pass direct coverage is in place for zero-score guards, positional count reduction, partition thresholds, fast-path partition scoring, pool merging, partition stats, and shared-position rut weighting. Remaining gaps are advanced playout/hard-mode branches. |
| `SolvleService` | Orchestration coverage is in place for English-vs-language fishing dictionary selection (incl. German routing), `hardMode`, `requireAnswer`, positional and non-positional scoring, game rating rows, invalid solve inputs, tuple scoring, `findBestNWords` require-answer behavior, `findSharedWordRestrictions`, the partitioning `getWordAnalysis` branch, `playOutSolutions` over the merged viable+fishing pool (incl. high-`guessNumber` failures), all three `solveDictionary` overloads (blank-firstWord via bestWords and via fishingWords, explicit firstWord, forced-starters with short-circuit and wrong-length/not-in-fishing-set rejection, and `previousGuesses` + `startingRestrictions`), and `submitTupleJob` cache hit, restart-after-FAILED, tiny-dictionary completion, the candidate `.map` block (via the expanded EXTENDED fixture), and the idle-timeout path (via the `maxJobIgnoreTimeSeconds` / `tupleJobTimeoutCheckInterval` seam). Intentional remaining gaps: defensive executor `catch` and the priority-queue swap-on-better-score `else` branches in `generateNWordListsHeuristic`. |
| `RemainingSolver` | Direct coverage is now in place via `RemainingSolverTest`: `getNextGuess` fishing/partition/viable-word branches, previous-guess avoidance, and the null terminal case, plus full `solve`/`solveWord` loop tests for solving to the answer, first-word-is-solution, prepended valid starters, and invalid/unknown-word rejections. |
| `SolvleController` | First-pass MockMvc coverage is in place for every active endpoint, default/query handling, lowercasing, tuple parsing, repeated guesses, and invalid enum handling. |
| `GameScoreDTO`, `SolveJob`, `PartitionStats`, `TupleScore`, `PlayOut`, `WordFrequencyScore` | First-pass unit coverage is in place. Remaining work is branch/edge coverage where it clarifies behavior. |
| `GroupSolver`, `SolvescapeService` | Classified experimental. Moved to `com.appsoil.solvle.experimental` and excluded from JaCoCo (`SolvescapeServiceTest` still runs but does not count toward coverage). |
| `PreloadService` | Removed. The startup listener body had been commented out and `SolvleService#preloadPartitionData` no longer exists; the bean was inert. |

Frontend validation passed with 1 Jest test.

Frontend Istanbul baseline:

| Metric | Coverage |
| --- | ---: |
| Statements | 22.70% |
| Branches | 14.09% |
| Functions | 19.55% |
| Lines | 22.89% |

Notable frontend coverage:

| Area | Current signal |
| --- | --- |
| `App.test.js` | Smoke-renders the app, mocks one initial `/solvle` fetch, and asserts `0 possible words`. |
| `Options.js` | Relatively high incidental coverage from initial render, but request/error/tab behavior is not thoroughly asserted. |
| `Board.js`, `contexts.js` | Covered by render only; little behavioral confidence. |
| `Controls.js`, `RateMyGame.js`, `RowScore.js`, `ScoreMyStarter.js`, `SolveModal.js`, `TupleCompletion.js` | 0% coverage. |
| `functions.js` | Partial coverage from app render. Needs direct unit tests for restriction strings, config params, and anagram strings. |

## Tooling Recommendations

### Backend

Add JaCoCo to `pom.xml` so coverage is available from normal Maven workflows and CI. Start by publishing the report without failing builds, then add checks once the P0 suite lands.

Recommended phases:

1. Add `jacoco-maven-plugin` with `prepare-agent` and `report`.
2. Add a CI artifact for `target/site/jacoco`.
3. Add `jacoco:check` with conservative ratchet thresholds after P0 tests are merged.
4. Prefer class/package thresholds for product code over a single global threshold.

Suggested initial post-P0 targets:

- Backend global line coverage: at least 65%.
- Backend global branch coverage: at least 50%.
- Core solver packages (`data`, `service`, `service.solvers`): at least 80% line coverage and 65% branch coverage.
- Controller package: each active endpoint has at least one success-path HTTP test and one validation/default-parameter test.

Use JUnit 5, parameterized tests, and Spring `MockMvc` for controller contracts. Keep slow full-dictionary and performance-style tests out of the default test phase by using a tag or Maven profile rather than `@Disabled`.

### Frontend

Use the existing Create React App Jest/Istanbul stack first:

1. Add a `test:coverage` script that wraps:

   ```powershell
   react-scripts test --watchAll=false --coverage --passWithNoTests
   ```

2. Keep using React Testing Library and `@testing-library/user-event`, both already present.
3. Exclude app wiring files such as `index.js`, `reportWebVitals.js`, and `setupProxy.js` from threshold decisions.
4. Add threshold checks after P0 tests land. Do not gate on the current 22.89% line baseline.

Suggested initial post-P0 targets:

- Frontend global line coverage: at least 60%.
- Frontend global branch coverage: at least 45%.
- No product component at 0% coverage.
- Direct tests for all fetch workflows.

Optional later tool: add a browser end-to-end suite, such as Playwright, for one local full-stack smoke path through typing a word, fetching suggestions, opening a modal, and verifying a backend response. This should complement unit/component tests, not replace them.

## Backend P0 Test Spec

These tests should exist before behavior changes to restrictions, scoring, solving, dictionaries, DTOs, or endpoints.

### Restriction Semantics

Add focused tests for `WordRestrictions` and `WordCalculationService#isValidWord`:

- Available letters only.
- Known positions with 1-based indexes.
- Required unknown letters.
- Position exclusions.
- Minimum letter frequencies via `^N`.
- Combined position plus frequency plus exclusion, for example `g5^2!2`.
- Duplicate-letter Wordle semantics where a gray duplicate must not remove a letter that is confirmed elsewhere.
- Preservation of existing restrictions when generating restrictions from a new guess.
- `withAdditionalLetterPositions` merges required letters and positions without losing exclusions or frequencies.

Done: these restriction-edge cases are now covered by `WordRestrictionsTest` (parsing, `generateRestrictions` including the gray-duplicate rule and `min(solution, guess)` minimum frequency, `withAdditionalLetterPositions` merges, and combined `isValidWord` checks).

### Scoring And Filtering

Add deterministic small-dictionary tests for:

- `calculateCharacterCounts` and `calculateCharacterCountsByPosition`, including duplicate letters.
- `removeRequiredLettersFromCountsByPosition` for known-position, one-option, and two-option branches.
- `calculateFreqScore` zero cases when total words or max score is zero.
- `calculateFreqScoreByPosition` with and without viable-word preference.
- Harmonic vs non-harmonic scoring.
- Vowel, uniqueness, location, and viable-word adjustment branches.
- Fishing words remain allowed to come from non-solution dictionaries when `requireAnswer=false`.
- `hardMode=true` filters fishing guesses through current restrictions.
- `requireAnswer=true` collapses fishing guesses to the solution set.

### Partition And Solver Behavior

Add tests for:

- `calculateRemainingWords` returns an empty set above threshold and partition scores at or below threshold.
- `wordsByRemainingGuesses` fast path for one or two viable words.
- `getPartitionStatsForWord` and `getPartitionStatsForTuple` group counts, average remaining words, and entropy.
- Done: `RemainingSolver#getNextGuess` chooses partition recommendations when present, avoids prior guesses, falls back to fishing or viable words, and handles terminal solution cases. Covered by `RemainingSolverTest`.
- `solveWord` returns `Word Not Found` for invalid solutions and `First word not valid` for invalid openers.
- `solveDictionary` forced starters are validated, prepended, and stop early when a starter is the solution.

### Service Orchestration

First-pass `SolvleService` tests now cover:

- `getWordAnalysis` English answer lists using `DictionaryType.BIG` for fishing while language dictionaries use their configured fishing sets.
- `hardMode=true` filtering fishing guesses through current restrictions.
- `requireAnswer=true` collapsing fishing guesses to the solution set.
- `getScore` returning finite score and partition stats for a candidate.
- `rateGame` returning one row per guess and tracking actual remaining words.
- `solveWord` rejecting unknown solutions and invalid first guesses.
- `scoreTuple` returning tuple partition stats.
- `findBestNWords` respecting `requireAnswer` when choosing available guesses.
- `solveDictionary` blank-firstWord branch picks the opener from analysis, explicit-firstWord branch uses the provided opener, and the forced-starters overload prepends starters in order, short-circuits when a starter equals the solution, and rejects wrong-length or not-in-fishing-set starters.
- `playOutSolutions` returns PlayOuts drawn from the merged viable + fishing pool.
- `submitTupleJob` returns the cached job on a repeat submit, replaces a FAILED cached job with a new id on the next submit, and lands at `COMPLETED` with a non-null result on a tiny dictionary.

Remaining `SolvleService` tests to add:

- `getWordAnalysis` with each meaningful config branch: simple scoring, positional scoring, no partitioning, partitioning, hard mode, and require-answer mode.
- `getScore` returns stable score and partition stats for restricted inputs.
- `rateGame` computes skill, luck, heuristic, and aggregate values across multiple rows and edge expected-remaining values.
- `submitTupleJob` idle-timeout path. Not currently testable without a refactor: the executor's `setStatus(RUNNING)` races with an externally-forced `FAILED`, and the 60-second `MAX_JOB_IGNORE_TIME_SECONDS` is a hardcoded private constant. Tracked as a follow-up — extract a settable timeout and inject a clock supplier before adding a deterministic assertion.

### Controllers And API Contracts

Keep and extend `MockMvc` tests for every active endpoint in `SolvleController`:

- `GET /solvle/{wordRestrictions}` lowercases restrictions and appends the original restriction string in the response.
- `GET /solvle/score/{wordRestrictions}/{wordToScore}` lowercases both path variables and passes shared query params.
- `GET /solvle/scoreTuple/{tupleString}` parses comma-separated tuples.
- `GET /solvle/{wordRestrictions}/best/{bestNWords}` returns tuple scores and passes `requireAnswer`.
- `GET /solvle/submitTupleJob/{tupleString}` returns a `SolveJob` response shape.
- `GET /solvle/{wordRestrictions}/playout` passes `guess`.
- `GET /solvle/solve/{solution}` accepts `firstWord`.
- `GET /solvle/rate/{solution}` accepts repeated `guesses`.
- Shared defaults for `wordList`, `wordConfig`, `hardMode`, and `requireAnswer`.
- Invalid enum or malformed inputs return a predictable Spring error response.

### Data Objects And Utility Classes

Add tests for product data classes:

- `Word`, `WordFrequencyScore`, `PartitionStats`, `TupleScore`, `PlayOut`, and `KnownPosition` ordering and equality.
- `SharedPositions#toKnownPositionDTOList`, recommendations, sorting, and description formatting.
- `GameScoreDTO#addRow` math and aggregate getter behavior with one row, multiple rows, and zero/edge expected remaining values.
- `SolveJob` default status, runtime, and mutable progress fields.

Classification decisions:

- `GroupSolver` and `SolvescapeService` were moved to `com.appsoil.solvle.experimental` and excluded from JaCoCo via a plugin-level `<excludes>` entry. They are not product code: `GroupSolver` is a Connections-style scratch tool with a hardcoded `main`, and `SolvescapeService` is wired as a bean but has no controller route and no frontend caller. The stale `/solvescape` proxy and unused `generateAnagramString` helper in the frontend are left in place so the anagram path can be revived later; if that does not happen, both should be removed during a future cleanup pass.
- `PreloadService` was deleted. The listener body had been commented out, the `SolvleService#preloadPartitionData` method it called no longer exists, and the bean was registered but inert under the `!test` profile.

## Backend P1 Test Spec

Add a small real-dictionary smoke suite that is not exhaustive:

- `SolvleConfig` loads every `DictionaryType` with non-empty expected word-size buckets.
- English solution lists use the broader valid-guess dictionary for fishing.
- Spanish, Icelandic, and German dictionary selections do not accidentally fall back to the English valid-guess list.
- One golden suggestion request per major dictionary returns a non-empty response.

## Full-Dictionary Benchmark And Exploration Suites

`FullDictionaryTest` was split into two tagged suites that are excluded from the default `mvn test` run:

- `com.appsoil.solvle.benchmark.DictionaryBenchmarkTest` (`@Tag("benchmark")`) — solve-quality regression. One test method per shipped `WordConfig` (six total), each runs `solveDictionary` against `DictionaryType.SIMPLE` with `hardMode=false, requireAnswer=true` through 1 warm-up + 3 measurement runs. Each test compares the resulting distribution and aggregates against a committed JSON baseline under `src/test/resources/benchmarks/`. Default tolerances: mean +0.02 guesses, max can't increase, failure count can't increase, no more than 0.5% of solutions can shift into a worse bucket. Runtime is reported and warns at +25% but never fails the test.
- `com.appsoil.solvle.service.DictionaryExplorationTest` (`@Tag("exploration")`) — manual playground organized into three `@Nested` classes: `StarterWordSearch`, `ParameterSweeps`, `OneShotInspection`. Asserts nothing; reads through logs to interpret results.

Run via:

```powershell
mvn -Pbenchmark test                                       # ~15-20 minutes; fails on solve-quality regression
mvn -Pbenchmark test -Dbenchmark.baseline.write=true       # accept current results as new baseline
mvn -Pexploration test                                     # unbounded; usually run individual @Nested classes
```

Updating a baseline is a deliberate human action — regenerate, inspect the JSON diff, commit with a message explaining the accepted regression.

Initial baselines (one full `mvn -Pbenchmark test -Dbenchmark.baseline.write=true` against `DictionaryType.SIMPLE`, 2,315 solutions, `hardMode=false, requireAnswer=true`, 1 warm-up + 3 measurement runs):

| Config | First word | Mean | Median | p95 | Max | Failures | Median runtime |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `SIMPLE` | alert | 3.7313 | 4.0 | 5.0 | 7 | 1 | 5.2s |
| `SIMPLE_WITH_PARTITIONING` | alert | 3.5952 | 4.0 | 5.0 | 6 | 0 | 15.1s |
| `OPTIMAL_MEAN` | slate | 3.5991 | 4.0 | 5.0 | 8 | 2 | 20.7s |
| `OPTIMAL_MEAN_WITH_PARTITIONING` (flagship) | slate | 3.4592 | 3.0 | 4.0 | 6 | 0 | 47.0s |
| `OPTIMAL_MEAN_EXTENDED_PARTITIONING` | raise | 3.5093 | 3.0 | 4.0 | 6 | 0 | 74.4s |
| `OPTIMAL_MEAN_WITH_PARTITIONING_HARMONIC` | slant | 3.4786 | 3.0 | 4.0 | 6 | 0 | 43.3s |
| `OPTIMAL_MEAN_WITH_PARTITIONING_HARD_MODE` (hardMode=true) | slate | 3.5400 | 3.0 | 5.0 | 8 | 7 | 15.7s |
| `OPTIMAL_MEAN_WITH_PARTITIONING_HARD_MODE_RUTBREAK` (hardMode=true) | slate | 3.5400 | 3.0 | 5.0 | 8 | 7 | 17.7s |
| `TWO_OR_LESS` | slate | 3.4786 | 3.0 | 4.0 | 6 | 0 | 27.1s |

Full benchmark run total wall time: ~12:43 on the developer laptop these baselines were captured on. Runtime numbers in committed baselines are informational — comparator warns at +25% but does not gate on runtime.

`OPTIMAL_MEAN_WITH_PARTITIONING` is the production flagship by solve quality on these baselines (mean 3.4592, max 6, 0 failures). `OPTIMAL_MEAN_EXTENDED_PARTITIONING` is retained as an experimental high-partition-threshold variant; its mean and runtime are both worse, but it's tracked so we notice if a future change closes that gap.

Findings from the harmonic + hard-mode benchmark sweep (initial values, ripe for tuning):

- The fixed harmonic series slightly underperforms the flagship (3.4786 vs 3.4592 mean), and picks a different opener (`slant` vs `slate`). Keep the feature exposed but don't make it default.
- Hard mode adds ~0.08 to mean and produces 7 failures (vs 0) on the SIMPLE answer list — expected, hard mode is harder.
- `withRutBreak(1.0, 6)` produces identical numbers to plain hard mode at these defaults. Likely the threshold of 6 shared-word-set members is too high for typical Wordle answer-list patterns; the early-return at the top of `generateSharedCharacterWeights` filters everything out. Tune `rutBreakThreshold` down (e.g., 3-4) or `rutBreakMultiplier` up to actually see weight effects.

Tuple-job idle-timeout coverage in `SolvleService` is still open and would require a small test seam (extract `MAX_JOB_IGNORE_TIME_SECONDS` and inject a clock supplier) before it can be tested deterministically.

## Frontend P0 Test Spec

These tests should exist before changing UI state handling, fetch contracts, or solver controls.

### Pure Functions

Add direct tests for `solvle-front/src/functions/functions.js`:

- `generateRestrictionString` with available letters only.
- Known letters produce position digits.
- Unsure letters produce `!` and excluded positions.
- Known plus unsure states serialize in backend-compatible order.
- Non-English allowable letters are preserved.
- `generateAnagramString` flattens filled board letters and ignores blanks.
- `generateConfigParams` includes `hardMode`, `requireAnswer`, `wordList`, `wordConfig`, and `wordLength`.

### Board And Keyboard

Add React Testing Library tests for:

- Physical keyboard entry.
- On-screen key entry.
- Enter does nothing until the row is full.
- Enter advances to the next row when full.
- Backspace deletes within a row and moves back to the previous row when appropriate.
- Board cannot overflow the configured word length or attempts.
- Reset Board restores board, row scores, available letters, known letters, unsure letters, and options.
- Exclude All removes letters currently on the board from availability.
- Clicking a tile cycles default, gray, yellow, green, and back to default.
- Duplicate-letter tile state changes preserve confirmed letters.
- `rateEnteredWords=true` fetches `/solvle/score/...` for completed rows and renders `RowScore`.

### Options And Suggestions

Add tests for:

- Initial suggestion fetch URL includes the generated restriction string and config params.
- Loading state appears before the response.
- Success state renders viable words, fishing words, total count, and optional partition tab.
- Error state renders the error placeholder data and stops loading.
- Changing dictionary, config, hard mode, require-answer mode, partition setting, or board restriction state triggers a new fetch.
- Selecting a suggested word fills the current row and advances the attempt.
- Partition tab is disabled or explanatory when `bestWords` is `null` or partitioning is off.

### Settings And Local Storage

Add tests for:

- Initial settings read `hardMode`, `requireAnswer`, `usePartitioning`, `rateEnteredWords`, `displayEntropy`, `wordConfig`, and `dictionary` from localStorage.
- Controls write setting changes back to localStorage.
- Closing the config modal toggles `shouldUpdate` so options refetch.
- Dictionary changes update keyboard layout.

### Utility Modals

Add tests for:

- `SolveModal` opens, sets solver-open state, calls `/solvle/solve/{solution}`, passes `firstWord`, renders returned guesses, and closes cleanly.
- `ScoreMyStarter` validates comma-separated 5-character words, blocks invalid input, calls `/solvle/scoreTuple/{tuple}`, and renders entropy and words remaining.
- `TupleCompletion` validates input, calls `/solvle/submitTupleJob/{tuple}`, polls until `COMPLETED`, renders progress, handles `FAILED`, supports cancel, and sorts by tuple, words remaining, and entropy.
- `RateMyGame` encodes repeated guesses, calls `/solvle/rate/{solution}`, renders per-row and aggregate metrics, handles loading/errors, and copies spoiler and spoiler-free summaries when clipboard is available.
- Utility dropdown interactions do not allow normal keyboard entry while a solver modal is open.

## Frontend P1 Test Spec

Add integration-style component tests for:

- A complete user path: type a guess, color tiles, fetch suggestions, select a suggestion, and reset.
- A completed game rating path with realistic backend fixture data.
- Tuple completion polling with fake timers.
- Accessibility basics for keyboard-only users: buttons have names, modals trap focus through React Bootstrap defaults, and tabs are reachable.

## CI And Acceptance Criteria

Before product changes begin:

1. Backend fast tests pass with JaCoCo report generation.
2. Frontend tests pass with Istanbul report generation.
3. Every active `/solvle` endpoint has at least one controller contract test.
4. Every active frontend fetch workflow has a mocked success and failure test.
5. No active product component remains at 0% frontend coverage.
6. No active product backend class remains at 0% line coverage, except intentional app wiring or explicitly classified experimental code.
7. Dictionary behavior has at least smoke coverage for solution-vs-fishing set selection.
8. Long-running full-dictionary tests are either tagged slow and runnable on demand or documented as manual exploration.
9. Coverage thresholds are added after the P0 tests land, starting from the improved baseline rather than the current baseline.
10. `docs/testing.md` is updated with the final coverage commands once tooling is committed.

## Open Decisions

- Should the `-Pbenchmark` profile run on a schedule in CI (nightly) once baselines stabilize, or stay developer-triggered? The 15-20 minute runtime makes per-PR gating unattractive; a scheduled run that posts a digest is the more likely shape.
- `rutBreak(1.0, 6)` produces identical numbers to plain hard mode at the current baseline. Threshold likely too high to actually trigger on real Wordle answer-list patterns. Worth a tuning pass (try threshold 3-4) to see whether the feature has any value before deciding its long-term fate.
- Frontend P0 tests are still unwritten (see spec above). Frontend is at ~23% line coverage. Likely its own branch.
- The stale `/solvescape` proxy and unused `generateAnagramString` helper in the frontend can be removed if anagram mode is not coming back. Otherwise, restoring a `SolvescapeController` is the missing piece.
