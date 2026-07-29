# Backend CI/CD

GitHub Actions runs CI for changes under `kapor-backend/` and deploys only a
successful `main` push to the production VM. Flutter is intentionally outside
this pipeline.

## One-time GitHub setup

Create a GitHub Environment named `production` under **Settings → Environments**.
Add a required reviewer there if deploys should need approval. Add these
environment secrets:

| Secret | Value |
| --- | --- |
| `VM_HOST` | VM hostname or IP address, without `https://` |
| `VM_USER` | Linux account used to deploy |
| `VM_DEPLOY_PATH` | Absolute repository path on the VM, e.g. `/opt/Kapor` |
| `VM_SSH_KEY` | Private key for the deploy-only SSH account |
| `VM_KNOWN_HOSTS` | Pinned host key output for the VM |

Never add `.env.local`, `JWT_SECRET`, or any application API key to GitHub
Actions YAML or repository secrets for this deployment. They remain in
`kapor-backend/.env.local` on the VM and are passed to Docker Compose there.

Generate the pinned host-key value from a trusted machine, verify its
fingerprint with the VM provider, then save the command output as the
`VM_KNOWN_HOSTS` secret:

```bash
ssh-keyscan -H your-vm-hostname
```

The public half of the SSH key belonging to `VM_SSH_KEY` must be in the deploy
user's `~/.ssh/authorized_keys` on the VM. The user needs permission to run
`git` in `VM_DEPLOY_PATH` and `docker compose` in `kapor-backend/`.

## What runs

`Backend CI` runs Maven verification and builds the `kapor-api` Docker image.
After a successful push to `main`, `Deploy Backend` SSHs to the VM, checks out
the exact commit that passed CI, rebuilds only `kapor-api`, and waits for
`/actuator/health` to succeed.

The deployment does not run seed scripts and does not deploy the Flutter app.
Use **Actions → Deploy Backend → Run workflow** for a deliberate redeploy of
the current `main` commit.
