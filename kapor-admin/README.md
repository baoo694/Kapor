# Kapor Admin

React/Vite admin panel, deployable as a small Nginx Docker image.

## Run on a Google Compute Engine VM

1. Create a Debian or Ubuntu Compute Engine VM with a **static external IP**.
   In the VPC firewall, allow inbound TCP `80` (and `443` when TLS is
   configured). Do **not** expose backend database, cache, object-storage, or
   NLP-service ports.
2. SSH to the VM and install Docker Engine with the Docker Compose plugin.
3. Clone this repository and enter the admin directory:

   ```bash
   git clone <YOUR_REPOSITORY_URL> kapor
   cd kapor/kapor-admin
   cp .env.production.example .env
   ```

4. Edit `.env` with the public HTTPS address of the Kapor backend:

   ```env
   VITE_API_URL=https://api.domday.food
   KAPOR_ADMIN_PORT=80
   ```

   `VITE_API_URL` is compiled into the JavaScript bundle and therefore public.
   Never place passwords, API keys, or other secrets in it.

5. Build and start the service:

   ```bash
   docker compose up -d --build
   docker compose ps
   ```

   Open `http://<VM_EXTERNAL_IP>/`. For production, point a DNS name at the
   static IP and terminate TLS with a reverse proxy or load balancer.

## Updating

```bash
git pull
docker compose up -d --build
```

Logs: `docker compose logs -f kapor-admin`.

## Local development

Copy `.env.example` to `.env.local`, set `VITE_API_URL` (for example
`http://localhost:8080`), then run `npm install && npm run dev`.
