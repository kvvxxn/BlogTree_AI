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

Run from the repository root, `BlogTree_AI`.

```bash
cp deploy/ec2/postgres.env.example deploy/ec2/postgres.env
cp deploy/ec2/api-main.env.example deploy/ec2/api-main.env
cp deploy/ec2/fastapi_worker.env.example deploy/ec2/fastapi_worker.env
```

Edit the three `.env` files and replace every `change-me` value.

Also edit `deploy/ec2/rabbitmq-definitions.json` and replace `change-me-rabbitmq-password` with the same RabbitMQ password used in:

- `deploy/ec2/api-main.env`
- `deploy/ec2/fastapi_worker.env`

RabbitMQ imports this definitions file on first boot. If you change the RabbitMQ password after the volume already exists, update the user from the RabbitMQ management UI or recreate the `rabbitmq_data` volume intentionally.

Then start the stack:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## Verify

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f api
docker compose -f docker-compose.prod.yml logs -f worker
curl https://54.153.236.215.nip.io/health
```

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
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

## Backup

At minimum, configure EC2 EBS snapshots. For logical database backups:

```bash
docker compose -f docker-compose.prod.yml exec postgres pg_dump -U blogtree_app blogtree > blogtree.sql
```
