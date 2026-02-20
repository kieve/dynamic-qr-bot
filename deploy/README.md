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
