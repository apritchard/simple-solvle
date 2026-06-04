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

## Initial Audit Baseline

Before the JaCoCo/tooling chunk, backend validation passed with 74 active tests and 1 skipped test:

- `SolvleServiceTest`: 58 tests.
- `WordCalculationServiceTest`: 14 tests.
- `SolvleApplicationTests`: 1 Spring context test.
- `FullDictionaryTest`: 1 skipped test class because the class is disabled.

Backend JaCoCo baseline from the initial audit:

| Metric | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 2,889 | 7,167 | 40.31% |
| Branches | 163 | 538 | 30.30% |
| Lines | 470 | 1,125 | 41.78% |

After adding JaCoCo to the Maven test phase and the first backend unit-test chunk, backend validation passes with 88 active tests and 1 skipped test:

| Metric | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 3,647 | 7,167 | 50.89% |
| Branches | 205 | 538 | 38.10% |
| Lines | 596 | 1,125 | 52.98% |

After adding the MockMvc controller-contract chunk, backend validation passes with 98 active tests and 1 skipped test:

| Metric | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 3,963 | 7,167 | 55.30% |
| Branches | 208 | 538 | 38.66% |
| Lines | 650 | 1,125 | 57.78% |

After adding the focused calculation-service chunk, backend validation passes with 104 active tests and 1 skipped test:

| Metric | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 4,287 | 7,167 | 59.82% |
| Branches | 233 | 538 | 43.31% |
| Lines | 697 | 1,125 | 61.96% |

Notable backend class coverage after the first three chunks:

| Area | Current signal |
| --- | --- |
| `WordRestrictions` | Strong line and branch coverage around parsing and generated restrictions. Needs more duplicate-letter golden cases. |
| `WordCalculationService` | First-pass direct coverage is in place for zero-score guards, positional count reduction, partition thresholds, fast-path partition scoring, pool merging, partition stats, and shared-position rut weighting. Remaining gaps are advanced playout/hard-mode branches. |
| `SolvleService` | Some restriction and solve flows covered with a six-word test dictionary. API orchestration, dictionary selection, rating, tuple jobs, and full config branches are undercovered. |
| `RemainingSolver` | Partially covered through service tests. Needs direct selection tests for viable, fishing, partition, repeated guesses, and terminal cases. |
| `SolvleController` | First-pass MockMvc coverage is in place for every active endpoint, default/query handling, lowercasing, tuple parsing, repeated guesses, and invalid enum handling. |
| `GameScoreDTO`, `SolveJob`, `SolvescapeService`, `PartitionStats`, `TupleScore`, `PlayOut`, `WordFrequencyScore` | First-pass unit coverage is in place. Remaining work is branch/edge coverage where it clarifies behavior. |
| `GroupSolver`, `PreloadService` | Still uncovered. Classify these before enforcing product coverage. |

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
- `RemainingSolver#getNextGuess` chooses partition recommendations when present, avoids prior guesses, falls back to fishing or viable words, and handles terminal solution cases.
- `solveWord` returns `Word Not Found` for invalid solutions and `First word not valid` for invalid openers.
- `solveDictionary` forced starters are validated, prepended, and stop early when a starter is the solution.

### Service Orchestration

Add `SolvleService` tests for:

- `getWordAnalysis` with each meaningful config branch: simple scoring, positional scoring, no partitioning, partitioning, hard mode, and require-answer mode.
- `getFishingSet` dictionary selection, especially English answer lists using `DictionaryType.BIG` for fishing and language-specific exceptions.
- `getScore` returns stable score and partition stats for unrestricted and restricted inputs.
- `rateGame` returns one row per guess and computes skill, luck, heuristic, and aggregate values.
- `scoreTuple` returns deterministic tuple stats.
- `findBestNWords` respects `requireAnswer`.
- `submitTupleJob` returns cached active jobs, restarts failed jobs, and eventually returns completed results. If this is hard to test reliably, introduce a small test seam for the executor and clock before asserting timeout behavior.

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

Classify these before writing tests:

- `GroupSolver`: either cover it as product behavior, move it behind an explicit experimental/slow test boundary, or remove it from product coverage expectations.
- `SolvescapeService`: either add the missing product/API coverage for anagrams or remove the stale `/solvescape` proxy expectation from frontend setup.
- `PreloadService`: decide whether startup preloading is product behavior worth asserting or just operational warmup.

## Backend P1 Test Spec

Add a small real-dictionary smoke suite that is not exhaustive:

- `SolvleConfig` loads every `DictionaryType` with non-empty expected word-size buckets.
- English solution lists use the broader valid-guess dictionary for fishing.
- Spanish, Icelandic, and German dictionary selections do not accidentally fall back to the English valid-guess list.
- One golden suggestion request per major dictionary returns a non-empty response.
- The disabled `FullDictionaryTest` scenarios are converted into tagged slow tests or documented as manual exploration only.

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

- Is `GroupSolver` still product code?
- Is `SolvescapeService` still intended to have an API route?
- Should full-dictionary solve quality become a scheduled/slow CI job, or remain manual exploration?
- What minimum acceptable solve performance should be asserted for each solver config?
- Should tuple jobs expose cancellation or timeout behavior as a tested public contract?
