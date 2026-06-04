# Agent Workflow

## Start Of Task
1. Check `git status --short --branch`.
2. Read the relevant backend or frontend files before editing.
3. Keep unrelated user changes intact.
4. Identify the smallest verification command that proves the change.

## Working Rules
- Prefer existing patterns over new abstractions.
- Keep backend and frontend API contracts synchronized.
- Do not reformat dictionaries or generated assets as cleanup.
- Do not stage `aws-backup/`, private keys, logs, or ignored build outputs.
- Use `npm.cmd` in PowerShell when `npm.ps1` is blocked.

## Common Task Paths
Backend solver or scoring changes:

```powershell
mvn test -q
```

Frontend changes:

```powershell
cd solvle-front
npm.cmd test -- --watchAll=false --passWithNoTests
npm.cmd run build
```

Full local app check:

```powershell
mvn spring-boot:run
cd solvle-front
npm.cmd start
```

Then open `http://localhost:3000` and verify that backend-backed interactions still work.

## AWS Backup Handling
The local `aws-backup/` directory may contain historical deployment metadata from the prior AWS hosting setup. For V1 onboarding, reference only that historical AWS context exists. Do not commit the backup, quote its identifiers, or use it as an implicit infrastructure spec. Future infrastructure work should begin with an explicit modern hosting/IaC plan.
