# Deploying & Running the QR Bot

## Deploy

From a Windows machine, run `deploy.bat` to build the JAR and SCP it to the server.

## One-Time Server Setup

Run `install.bat` from a Windows machine. This will:
- Copy `run_qr.sh`, `start_qr.sh`, `stop_qr.sh`, `restart_qr.sh`, `qr-bot.service`, `Caddyfile`, and `application-prod.yaml` to the server
- Install and enable the systemd service
- Install Caddy and configure it as a reverse proxy (HTTPS with auto-provisioned Let's Encrypt certs)

Then deploy the JAR with `deploy.bat` and start with `~/start_qr.sh` on the server.

**Prerequisites:** Ports 80 and 443 must be open in your cloud provider's firewall/security group.

## Day-to-Day Usage

| Action | Command |
|---|---|
| Start | `~/start_qr.sh` |
| Stop | `~/stop_qr.sh` |
| Restart (after deploy) | `~/restart_qr.sh` |
| Check status | `systemctl status qr-bot` |
| View logs | `journalctl -u qr-bot -f` |

## Future Improvement: Docker Compose Deployment

The current approach (SCP a JAR, manage systemd and Caddy manually) works fine for a single server but could be replaced with Docker Compose for a more self-contained setup. The idea: build a Docker image locally, transfer it to the server as a tarball, and let `docker-compose.yml` manage the app + Caddy together. No registry or source code on the server required.

### What you'd need

**Local machine:**
- Docker Desktop installed

**Server:**
- Docker and Docker Compose installed (`apt install docker.io docker-compose-plugin`)
- No Java, Caddy, or systemd service needed — Docker handles everything

### Files to create

**`Dockerfile`** (project root) — multi-stage build that produces a minimal image:

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/dynamic-qr-bot-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application-prod.yaml application-prod.yaml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

**`docker-compose.yml`** (on the server, or SCP it once during setup):

```yaml
services:
  app:
    image: qr-bot:latest
    restart: unless-stopped
    volumes:
      - ./qr-config.yaml:/app/qr-config.yaml
    ports:
      - "8080:8080"

  caddy:
    image: caddy:2
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
      - caddy_config:/config

volumes:
  caddy_data:
  caddy_config:
```

### Deploy workflow

**One-time server setup:**

```bash
# SCP the compose file, Caddyfile, and config to the server
scp docker-compose.yml Caddyfile qr-config.yaml root@server:~/qr-bot/
```

**Build and deploy (replaces `deploy.bat`):**

```bash
# 1. Build the image locally
docker build -t qr-bot .

# 2. Export to a tarball
docker save qr-bot | gzip > qr-bot.tar.gz

# 3. Transfer to the server
scp qr-bot.tar.gz root@server:~/qr-bot/

# 4. Load and restart on the server
ssh root@server "docker load < ~/qr-bot/qr-bot.tar.gz && cd ~/qr-bot && docker compose up -d"
```

### Day-to-day usage (Docker version)

| Action | Command (on server) |
|---|---|
| Start | `cd ~/qr-bot && docker compose up -d` |
| Stop | `cd ~/qr-bot && docker compose down` |
| Restart | `cd ~/qr-bot && docker compose restart app` |
| View logs | `cd ~/qr-bot && docker compose logs -f app` |
| Check status | `docker compose ps` |

### What this buys you

- **No server-side Java install** — the JRE is baked into the image
- **Caddy managed alongside the app** — one `docker compose down` stops everything
- **Reproducible** — the exact same image runs everywhere
- **Easy rollback** — keep previous tarballs and `docker load` an older one
- **Simpler server setup** — just Docker, no systemd units or shell scripts
