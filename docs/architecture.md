# Architecture

## Overview
Solvle is split into a Spring Boot backend and a React frontend.

The frontend collects puzzle state, encodes it into Solvle restriction strings, and calls backend endpoints under `/solvle`. The backend loads bundled dictionaries, applies Wordle-style restrictions, scores candidate words, and returns suggestions or solve results.

## Backend
- `SolvleController` exposes HTTP endpoints under `/solvle`.
- `SolvleService` coordinates dictionary selection, restriction filtering, scoring, tuple analysis, playouts, and solving.
- `WordCalculationService` handles scoring and frequency calculations.
- `SolvleConfig` loads dictionaries from `src/main/resources/dict2/`.
- Solver implementations live under `service/solvers/`.

The backend runs on port `8081`.

## Frontend
- `solvle-front/src/App.js` owns the main game state and several fetch workflows.
- Components under `solvle-front/src/components/` render the board, keyboard, options, solver modals, tuple completion, and scoring tools.
- `solvle-front/src/setupProxy.js` forwards `/solvle` and `/solvescape` during local development.

The frontend dev server runs on port `3000`.

## API Flow
Common frontend calls include:

- `/solvle/{wordRestrictions}` for suggestions and remaining valid words.
- `/solvle/score/{wordRestrictions}/{wordToScore}` for scoring a candidate word.
- `/solvle/scoreTuple/{tupleString}` for starter tuple scoring.
- `/solvle/solve/{solution}` for solving a known solution.
- `/solvle/rate/{solution}` for rating a completed game.

Keep frontend fetch parameters and backend controller defaults aligned when changing API behavior.

## Data Notes
Dictionary files are application data and should be changed only with a clear task and tests. See `docs/api-contracts.md` for the restriction-string and DTO contracts that connect frontend state to backend solver behavior.
