# Contributing

## Start Here
Solvle is a Spring Boot backend with a Create React App frontend. Use `AGENTS.md` for agent-specific guardrails and `docs/` for project details.

## Local Verification
Backend:

```powershell
mvn test -q
```

Frontend from `solvle-front/`:

```powershell
npm.cmd test -- --watchAll=false --passWithNoTests
$env:CI='true'; npm.cmd run build
```

Use `npm.cmd` in PowerShell if `npm.ps1` is blocked by local script policy.

## Pull Requests
- Keep unrelated local work out of the commit.
- Document frontend/backend API contract changes.
- Add or update tests for solver, restriction-string, scoring, API, or UI behavior changes.
- Do not stage `aws-backup/`, private keys, logs, or build output.

## Runtime Policy
The backend source level is Java 18, but Java 21 is the standard local and CI validation runtime. The frontend Dockerfile and CI currently use Node 17.
