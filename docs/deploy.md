# Deploying Solvle to AWS Lightsail Containers

Solvle ships as a **single container** (Spring Boot serves the React build on port
`8081`, API and UI share one origin) running on an **AWS Lightsail container service**.

- **Per-push deploys** are automated by `.github/workflows/deploy.yml` (build → push image → roll out).
- **Infrastructure** is a single managed service you create/destroy with a couple of
  `aws lightsail` commands (Lightsail has no first-class CloudFormation/CDK resource).
- **HTTPS** is bundled — Lightsail gives the service a managed `https://` URL out of the box.

Why Lightsail Containers over ECS Fargate: for a single small app the ALB (~$16/mo) is
half the bill, ECS doesn't scale to zero natively, and a managed Lightsail service is
cheaper and simpler. See the chosen tier and trade-offs below.

| Setting | Value |
| --- | --- |
| Region | `us-east-1` |
| Service name | `solvle` |
| Power (size) | `micro` (0.5 vCPU / 1 GB, ~$10/mo) — resize to `small` if analysis endpoints feel slow |
| Scale (nodes) | `1` |
| Container port | `8081` |
| Health check | `GET /actuator/health` (Spring Boot Actuator) |

> Replace `<ACCOUNT_ID>` with your AWS account id and `apritchard/simple-solvle` with
> your repo if it differs. Commands assume the AWS CLI v2 is installed and authenticated
> with admin-ish rights for the one-time setup.

---

## One-time setup

Do these once, in order. Steps 1–2 let GitHub deploy without storing AWS keys; step 3
creates the service; step 4 is the first deploy.

> **Windows / PowerShell:** the commands below use bash-style `\` line continuations.
> In PowerShell either put each command on a single line, or use a backtick `` ` `` instead
> of `\` at line ends. For the JSON files, use the `Set-Content ... -Encoding ascii`
> here-string approach (avoids a UTF-8 BOM the AWS CLI can choke on) rather than `Out-File`.

### 1. GitHub OIDC trust + deploy role

Create the GitHub OIDC provider (skip if another repo already created it in this account):

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

> AWS no longer validates the thumbprint for GitHub's provider, but the CLI still requires
> the argument; the value above is the long-standing GitHub one. (Alternatively, create the
> provider once in the IAM console's "Add provider" flow, which fetches the thumbprint for
> you, and skip this command.)

The next two steps each save a small JSON file locally, then pass it to the AWS CLI with
the `file://` prefix (the CLI reads the file from your current directory — run the
`create-role` command from the same folder you save these in). They're throwaway; delete
them once the role exists.

Save the following as **`trust.json`** (scopes the role to this repo; replace `<ACCOUNT_ID>`
with your 12-digit account id — `aws sts get-caller-identity --query Account --output text`):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike": { "token.actions.githubusercontent.com:sub": "repo:apritchard/simple-solvle:*" }
    }
  }]
}
```

> Tighten `:*` to `:ref:refs/heads/main` to allow only main-branch deploys.

Save the following as **`lightsail-deploy.json`** (least-privilege permissions for the
workflow; Lightsail actions don't support resource scoping, so `Resource` is `*`). No
placeholders here — paste it as-is:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "lightsail:CreateContainerServiceRegistryLogin",
      "lightsail:RegisterContainerImage",
      "lightsail:GetContainerImages",
      "lightsail:CreateContainerServiceDeployment",
      "lightsail:GetContainerServices",
      "lightsail:GetContainerServiceDeployments",
      "lightsail:GetContainerLog"
    ],
    "Resource": "*"
  }]
}
```

Create the role and attach the policy:

```bash
aws iam create-role --role-name gha-lightsail-deploy \
  --assume-role-policy-document file://trust.json

aws iam put-role-policy --role-name gha-lightsail-deploy \
  --policy-name lightsail-deploy --policy-document file://lightsail-deploy.json
```

### 2. Tell GitHub the role ARN

```bash
gh secret set AWS_DEPLOY_ROLE_ARN --body "arn:aws:iam::<ACCOUNT_ID>:role/gha-lightsail-deploy"
```

### 3. Create the container service

```bash
aws lightsail create-container-service \
  --region us-east-1 \
  --service-name solvle \
  --power micro \
  --scale 1
```

Wait until it's ready (state `READY`):

```bash
aws lightsail get-container-services --region us-east-1 \
  --service-name solvle --query "containerServices[0].state" --output text
```

### 4. First deploy

Push to `main`, or trigger the workflow by hand:

```bash
gh workflow run Deploy
```

Once the deployment is `ACTIVE`, get the public URL:

```bash
aws lightsail get-container-services --region us-east-1 \
  --service-name solvle --query "containerServices[0].url" --output text
# e.g. https://solvle.abc123.us-east-1.cs.amazonlightsail.com
curl -fsS "$(aws lightsail get-container-services --region us-east-1 \
  --service-name solvle --query 'containerServices[0].url' --output text)/actuator/health"
# -> {"status":"UP"}
```

---

## Day-to-day

- **Deploy:** just push to `main`. The workflow builds, pushes, and rolls out with a
  health-checked swap — bad builds don't take traffic.
- **Manual deploy / re-deploy:** `gh workflow run Deploy`.
- **Logs:** `aws lightsail get-container-log --region us-east-1 --service-name solvle --container-name app`
- **Rollback:** re-run the workflow on a known-good commit, or redeploy a previous image
  version (`aws lightsail get-container-images --service-name solvle` lists `:solvle.app.N`
  refs; feed an older one back through `create-container-service-deployment`).

## Resize (more juice)

The interactive solver is light, but the analysis endpoints (`/best`, `/playout`,
`submitTupleJob`) are CPU-bound. If they feel slow, bump the power — it's a live redeploy,
no rebuild:

```bash
aws lightsail update-container-service --region us-east-1 \
  --service-name solvle --power small   # 1 vCPU / 2 GB, ~$20/mo
```

Sizes: `nano` ($7, 0.5GB — too tight for this JVM), `micro` ($10, 1GB, **default**),
`small` ($20, 2GB), `medium` ($40, 4GB).

## Custom domain + HTTPS (when ready)

Lightsail issues and renews a managed TLS cert for you — no ACM, no manual cert wrangling.

```bash
# 1. Request a managed cert
aws lightsail create-certificate --region us-east-1 \
  --certificate-name solvle-cert --domain-name solvle.example.com

# 2. Add the validation CNAME it returns at your DNS registrar, then wait for ISSUED:
aws lightsail get-certificates --region us-east-1 \
  --query "certificates[?certificateName=='solvle-cert'].certificateDetail.status"

# 3. Attach the cert + domain to the service
aws lightsail update-container-service --region us-east-1 \
  --service-name solvle \
  --public-domain-names '{"solvle-cert":["solvle.example.com"]}'

# 4. Point your domain at the service: CNAME solvle.example.com -> the host part of the
#    service URL from `get-container-services` (drop the https:// and trailing slash).
```

> The app uses no client-side router, so Spring serving `index.html` at `/` is enough.
> If you later add client-side routes (deep links), add a controller that forwards unknown
> non-API paths to `index.html`.

---

## Teardown (destroy everything from the CLI)

```bash
# The service (stops all charges for compute)
aws lightsail delete-container-service --region us-east-1 --service-name solvle

# Optional: the managed cert, if you created one
aws lightsail delete-certificate --region us-east-1 --certificate-name solvle-cert

# The GitHub deploy role
aws iam delete-role-policy --role-name gha-lightsail-deploy --policy-name lightsail-deploy
aws iam delete-role --role-name gha-lightsail-deploy

# Optional: the OIDC provider (only if no other repo/role uses it)
aws iam delete-open-id-connect-provider \
  --open-id-connect-provider-arn arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com
```

---

## Local development

Same image that ships to Lightsail:

```bash
docker compose up --build        # http://localhost:8081
# or
docker build -t solvle:local . && docker run --rm -p 8081:8081 solvle:local
```

---

## Decommissioning the old EC2 setup

The legacy deploy was a single EC2 box + docker-compose, fronted by the `solvle-balancer`
ALB, deployed via SSH (`update-ec2.sh` / `restart-solvle.sh`, now removed). Once the
Lightsail service is serving traffic:

1. Repoint your domain from the old ALB to the Lightsail service (see custom-domain steps).
2. Terminate the EC2 instance, delete the ALB + target group, and release the Elastic IP.
3. **Rotate/delete the `new-solvle-pair.pem` SSH key** — it sits untracked in the working
   tree (gitignored, never committed) but is a live credential for the old box; remove it
   from disk once the instance is gone.
4. The `aws-backup/` folder is a local restore reference for the old instance; keep or
   delete it as you like (it's gitignored).
