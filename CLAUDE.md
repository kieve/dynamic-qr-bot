# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Dynamic QR Code Router Bot — a Spring Boot 4.0.2 application (Java 25, Gradle 9.3) that serves as both a REST API for QR code URL redirection and a Discord bot for managing those redirects.

## Build & Run Commands

```bash
./gradlew build        # Build project and create JAR
./gradlew bootRun      # Run the application
./gradlew test         # Run tests
```

The built JAR lands at `build/libs/dynamic-qr-bot-0.0.1-SNAPSHOT.jar`.

## Architecture

The application has two main entry surfaces sharing a common config layer:

- **QrRedirectController** — REST endpoint `GET /{path}` that performs HTTP 302 redirects to mapped destination URLs (404 if not found).
- **DiscordBotService** — JDA 6.3.1 Discord bot that auto-discovers and registers all `SlashCommand` implementations at startup. Dispatches slash-command and auto-complete events by command name.
- **BotConfigService** — Central config management backed by a YAML file (`qr-config.yaml`). Maintains ConcurrentHashMap indexes by `staticPath` and `nickname` for O(1) lookups. Mutations are synchronized; indexes are rebuilt on every update.

### Slash Commands (`slash` package)

Each Discord slash command is a Spring `@Component` implementing the `SlashCommand` interface. `DiscordBotService` collects all implementations via `List<SlashCommand>` injection and registers them with JDA automatically — adding a new command only requires creating a new class in the `slash` package.

- **`SlashCommand`** — Interface defining `getCommandData()`, `handle()`, and an optional `handleAutoComplete()`.
- **`UpdateQrCommand`** — `/update-qr`: changes a QR mapping's destination URL. Supports nickname auto-complete. Requires admin or global-admin.
- **`AddToWhitelistCommand`** — `/add-to-whitelist`: grants admin access to a Discord user. Requires global-admin.

All command responses are ephemeral.

Data models (`QrMapping`, `BotConfigFile`) are Java records. Configuration binding uses `@ConfigurationProperties` via `QrBotProperties`.

## Configuration

Key properties in `application.yaml` (or profile-specific overrides):
- `qr-bot.config-file` — path to runtime YAML config (default: `./qr-config.yaml`)
- `qr-bot.global-admin` — Discord user ID for global admin
- `qr-bot.discord-token` — Discord bot token

Local/prod profiles (`application-local.yaml`, `application-prod.yaml`) are gitignored. See `src/main/resources/qr-config-example.yaml` for the runtime config format.

## Code Conventions

- Private fields use `m_` prefix (e.g., `m_jda`, `m_configService`)
- Constructor injection throughout
- `@PostConstruct` / `@PreDestroy` for lifecycle management
- No database — all persistence is YAML file-based via Jackson
