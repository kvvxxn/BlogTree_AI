# EC2 Docker Compose Deployment

This deployment runs Caddy, Spring API, FastAPI worker, RabbitMQ, and PostgreSQL on one EC2 host.

## Domain

- Frontend: `https://blog-tree-ai.vercel.app`
- API: `https://54.153.236.215.nip.io`

`nip.io` resolves the IP from the hostname, so no separate DNS record is required.

## Required EC2 Inbound Rules

Open only these ports to the internet:

- `80/tcp`
- `443/tcp`
- `22/tcp` from your IP

Do not expose `8080`, `8000`, `5432`, `5672`, or `15672` publicly. The Compose services communicate on the internal Docker network.

## First Deploy

Run from the repository root, `BlogTree_AI`. Prepare these files on the production host:

- `deploy/env/postgres.env`
- `deploy/env/api-main.env`
- `deploy/env/fastapi_worker.env`
- `deploy/env/rabbitmq-definitions.json`
- `deploy/env/Caddyfile`

Edit the three `.env` files and replace every local placeholder value.

Also edit `deploy/env/rabbitmq-definitions.json` and set the RabbitMQ password to the same value used in:

- `deploy/env/api-main.env`
- `deploy/env/fastapi_worker.env`

RabbitMQ imports this definitions file on first boot. If you change the RabbitMQ password after the volume already exists, update the user from the RabbitMQ management UI or recreate the `rabbitmq_data` volume intentionally.

Then start the stack:

```bash
docker compose -f deploy/docker-compose.yml up -d
```

## Verify

```bash
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f api
docker compose -f deploy/docker-compose.yml logs -f worker
curl https://54.153.236.215.nip.io/health
```

## GitHub Actions CD

The backend CD workflow publishes these images to GHCR and restarts the `api` and `worker` services on the production host:

- `ghcr.io/<github-owner>/blogtree-api:sha-<commit>`
- `ghcr.io/<github-owner>/blogtree-worker:sha-<commit>`

Set these repository secrets before enabling production deployment:

- `PROD_SSH_HOST`: production host address
- `PROD_SSH_USER`: SSH user
- `PROD_SSH_KEY`: private SSH key for the user
- `PROD_DEPLOY_PATH`: absolute path to the checked-out `BlogTree_AI` repository on the host
- `PROD_SSH_PORT`: optional, defaults to `22`
- `GHCR_READ_TOKEN`: optional if GHCR packages are public or the host is already logged in; otherwise use a PAT with `read:packages`
- `GHCR_USERNAME`: optional GHCR username for `GHCR_READ_TOKEN`; defaults to the GitHub repository owner

## Vercel Environment Variables

Set these in the Vercel project and redeploy the frontend:

```env
VITE_API_BASE_URL=https://54.153.236.215.nip.io
VITE_GOOGLE_CLIENT_ID=your-google-client-id
VITE_GOOGLE_REDIRECT_URI=https://blog-tree-ai.vercel.app/auth/callback
```

## Google OAuth

Add these redirect URIs in Google Cloud Console:

```text
https://54.153.236.215.nip.io/login/oauth2/code/google
https://blog-tree-ai.vercel.app/auth/callback
```

## Update Deploy

```bash
git pull
docker compose -f deploy/docker-compose.yml pull api worker
docker compose -f deploy/docker-compose.yml up -d api worker
docker image prune -f
```

## Backup

At minimum, configure EC2 EBS snapshots. For logical database backups:

```bash
docker compose -f deploy/docker-compose.yml exec postgres pg_dump -U blogtree_app blogtree > blogtree.sql
```
