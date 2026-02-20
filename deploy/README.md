# Deploying & Running the QR Bot

## Deploy

From a Windows machine, run `deploy.bat` to build the JAR and SCP it to the server.

## One-Time Server Setup

Copy `run_qr.sh` and `qr-bot.service` to the server, then:

```bash
chmod +x ~/run_qr.sh
cp qr-bot.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable qr-bot
systemctl start qr-bot
```

## Day-to-Day Usage

| Action | Command |
|---|---|
| Start | `systemctl start qr-bot` |
| Stop | `systemctl stop qr-bot` |
| Restart (after deploy) | `systemctl restart qr-bot` |
| Check status | `systemctl status qr-bot` |
| View logs | `journalctl -u qr-bot -f` |
