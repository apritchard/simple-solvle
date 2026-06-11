# Test Coverage Spec

This document tracks what test coverage must exist before changing solver or UI behavior, plus open testing decisions. Coverage percentages are useful for finding blind spots, but the target is behavioral confidence: restriction semantics, dictionary selection, scoring, API contracts, and the main UI workflows should each have direct tests.

Do not trust coverage numbers written in docs — generate current ones:

```powershell
mvn test -q                                                  # backend; JaCoCo report at target/site/jacoco/index.html
cd solvle-front
npm.cmd test -- --watchAll=false --coverage --passWithNoTests # frontend Istanbul report
```

Benchmark and exploration suites are documented in [testing.md](testing.md).

## Current State

### Backend — P0 complete, gated
JaCoCo runs on every `mvn test`, and `jacoco:check` in `pom.xml` (authoritative for the thresholds) fails the build below minimum instruction/branch/line coverage. The experimental package (`com.appsoil.solvle.experimental`) is excluded.

The backend P0 suite is done: restriction parsing/generation edges (`WordRestrictionsTest`), calculation-service scoring and partition branches, `SolvleService` orchestration (dictionary routing, hard mode, require-answer, playouts, all `solveDictionary` overloads, `submitTupleJob` lifecycle including the idle-timeout path), `RemainingSolver` solve loops, MockMvc contracts for every active endpoint, and data-object ordering/equality.

Intentional remaining gaps:

- The defensive executor `catch` for a failed tuple-job runnable in `SolvleService`.
- The two priority-queue swap-on-better-score `else` branches in `generateNWordListsHeuristic` (needs a ~65+ word fixture to exceed `TOP_N`).

### Frontend — P0 partially done
Completed buckets: pure functions in `functions.js` (100%), single-component tests via the `renderWithContext` helper in `testUtils.js` (`Letter`, `Key`, `OptionTab`, `RowScore`, `SolvleAlert`), and two fetch-mocked modals (`SolveModal`, `ScoreMyStarter`).

Still unwritten (the specs below): board/keyboard interaction, options/suggestions fetch workflows, settings/localStorage, `TupleCompletion`, `RateMyGame`, and `App.js` integration. No frontend coverage thresholds are enforced yet; add them after the remaining P0 buckets land.

### Classification decisions
- `GroupSolver` and `SolvescapeService` are experimental, not product code: `GroupSolver` is a Connections-style scratch tool with a hardcoded `main`; `SolvescapeService` has no controller route and no frontend caller. The stale `/solvescape` proxy entry and unused `generateAnagramString` frontend helper are kept so the anagram path can be revived; remove all of it if that never happens.
- `PreloadService` was deleted — its listener body was commented out and the bean was inert.

### Findings worth keeping
- `WordCalculationService#harmonic` was broken for years (`1.0/n` instead of `1.0/i`); the fix is benchmarked as `OPTIMAL_MEAN_WITH_PARTITIONING_HARMONIC` and slightly underperforms the flagship, so it is exposed but not default.
- The three rutBreak call sites in `SolvleService` are guarded no-ops for all pre-existing configs (`rutBreakThreshold > 1`, `rutBreakMultiplier > 0`); `withRutBreak(1.0, 6)` measured identical to plain hard mode — the threshold is likely too high to trigger on real Wordle answer lists.
- The tuple-job idle-timeout path had a latent bug (`finishTuple` unconditionally overwrote `FAILED` with `COMPLETED`); fixed, with `maxJobIgnoreTimeSeconds` / `tupleJobTimeoutCheckInterval` as package-private test seams.
- Fixture ordering traps with `Word` / `WordFrequencyScore` sets are documented in [testing.md](testing.md) under Backend Fixture Notes.

## Backend P1 Test Spec (open)
Add a small real-dictionary smoke suite that is not exhaustive:

- `SolvleConfig` loads every `DictionaryType` with non-empty expected word-size buckets.
- English solution lists use the broader valid-guess dictionary for fishing.
- Spanish, Icelandic, and German dictionary selections do not accidentally fall back to the English valid-guess list.
- One golden suggestion request per major dictionary returns a non-empty response.

## Frontend P0 Test Spec (open buckets)
These tests should exist before changing UI state handling, fetch contracts, or solver controls.

### Board And Keyboard
- Physical keyboard entry and on-screen key entry.
- Enter does nothing until the row is full; advances to the next row when full.
- Backspace deletes within a row and moves back to the previous row when appropriate.
- Board cannot overflow the configured word length or attempts.
- Reset Board restores board, row scores, available letters, known letters, unsure letters, and options.
- Exclude All removes letters currently on the board from availability.
- Clicking a tile cycles default, gray, yellow, green, and back to default.
- Duplicate-letter tile state changes preserve confirmed letters.
- `rateEnteredWords=true` fetches `/solvle/score/...` for completed rows and renders `RowScore`.

### Options And Suggestions
- Initial suggestion fetch URL includes the generated restriction string and config params.
- Loading state appears before the response.
- Success state renders viable words, fishing words, total count, and optional partition tab.
- Error state renders the error placeholder data and stops loading.
- Changing dictionary, config, hard mode, require-answer mode, partition setting, or board restriction state triggers a new fetch.
- Selecting a suggested word fills the current row and advances the attempt.
- Partition tab is disabled or explanatory when `bestWords` is `null` or partitioning is off.

### Settings And Local Storage
- Initial settings read `hardMode`, `requireAnswer`, `usePartitioning`, `rateEnteredWords`, `displayEntropy`, `wordConfig`, and `dictionary` from localStorage.
- Controls write setting changes back to localStorage.
- Closing the config modal toggles `shouldUpdate` so options refetch.
- Dictionary changes update keyboard layout.

### Remaining Utility Modals
- `TupleCompletion` validates input, calls `/solvle/submitTupleJob/{tuple}`, polls until `COMPLETED`, renders progress, handles `FAILED`, supports cancel, and sorts by tuple, words remaining, and entropy.
- `RateMyGame` encodes repeated guesses, calls `/solvle/rate/{solution}`, renders per-row and aggregate metrics, handles loading/errors, and copies spoiler and spoiler-free summaries when clipboard is available.
- Utility dropdown interactions do not allow normal keyboard entry while a solver modal is open.

## Frontend P1 Test Spec (open)
Add integration-style component tests for:

- A complete user path: type a guess, color tiles, fetch suggestions, select a suggestion, and reset.
- A completed game rating path with realistic backend fixture data.
- Tuple completion polling with fake timers.
- Accessibility basics for keyboard-only users: buttons have names, modals trap focus through React Bootstrap defaults, and tabs are reachable.

Optional later tool: a browser end-to-end suite, such as Playwright, for one full-stack smoke path (type a word, fetch suggestions, open a modal, verify a backend response) against the single combined container. Complement to unit/component tests, not a replacement.

## Solver-Ranking Note
Partition ranking is **entropy-first by design**, driven by `WordFrequencyScore.compareTo` (`data/WordFrequencyScore.java`). The TreeSet that backs `SolvleDTO.bestWords` orders by `partitionStats.entropy()` (highest first), and only falls back to the `freqScore` field when entropies tie. The `freqScore` expression in `WordCalculationService.wordsByRemainingGuesses` (`(1 - wordsRemaining/N) + viableWordPreference`) is therefore an entropy-tied tiebreaker — not the primary rank. The `DataModelTest#wordFrequencyScore_sortByPartitionEntropyThenScoreThenNaturalOrder` test pins this behavior.

Implication for solver-quality work: any "switch to entropy ranking" suggestion is already true; the real lever for closing the gap to SOTA solvers is the **partition-candidate pool**, not the ranking criterion. The pool that gets entropy-evaluated is the merged top-`MAX_RESULT_LIST_SIZE`(100) viable + top-`FISHING_WORD_SIZE`(200) fishing words filtered by positional-frequency score; candidates not in that pool — including some openers like "salet" — never get entropy-ranked at all. Widening `FISHING_WORD_SIZE` exponentially increases first-move latency, which is a UX constraint; a cleaner direction is one-time precomputation of the optimal first guess per `(WordConfig, DictionaryType)` pair.

## Open Decisions
- Should the `-Pbenchmark` profile run on a schedule in CI (nightly) once baselines stabilize, or stay developer-triggered? The 15-20 minute runtime makes per-PR gating unattractive; a scheduled run that posts a digest is the more likely shape.
- `rutBreak(1.0, 6)` produces identical numbers to plain hard mode at the current baseline. Threshold likely too high to actually trigger on real Wordle answer-list patterns. Worth a tuning pass (try threshold 3-4) to see whether the feature has any value before deciding its long-term fate.
- Frontend P0 buckets above are still unwritten. Likely its own branch.
- The stale `/solvescape` proxy and unused `generateAnagramString` helper in the frontend can be removed if anagram mode is not coming back. Otherwise, restoring a `SolvescapeController` is the missing piece.
