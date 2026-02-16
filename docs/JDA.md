# JDA 6.3.1 - Slash Command Registration

## Guild-Scoped Commands

Use `guild.updateCommands()` which returns a `CommandListUpdateAction`:

```java
guild.updateCommands().addCommands(
    Commands.slash("echo", "Repeats messages back to you.")
        .addOption(OptionType.STRING, "message", "The message to repeat.")
).queue();
```

**Note:** `updateCommands()` replaces ALL commands — commands not included will be deleted.

`addCommands()` accepts varargs `CommandData...` or `Collection<? extends CommandData>`.

**Limits:** Max 100 slash commands per guild.

## Global Commands

### Update all at once
```java
jda.updateCommands().addCommands(
    Commands.slash("ping", "Get the bot's latency")
).queue();
```

### Upsert individual command (create or update)
```java
jda.upsertCommand(Commands.slash("ping", "Get the bot's latency")).queue();
```

`upsertCommand()` is idempotent — if a command with the same name exists, it will be replaced.

Alternative overload:
```java
jda.upsertCommand("name", "description").queue();
```

## Key Types

- **CommandData** - base interface for all application commands
- **SlashCommandData** - extends CommandData, returned by `Commands.slash()`
- **CommandListUpdateAction** - fluent builder returned by `updateCommands()`

## Requirements

Bot needs OAuth2 scopes: `bot` + `applications.commands`

Commands persist after bot restarts — no need to re-register every startup.

## Sources

- https://docs.jda.wiki/net/dv8tion/jda/api/entities/Guild.html
- https://docs.jda.wiki/net/dv8tion/jda/api/JDA.html
- https://jda.wiki/using-jda/interactions/
- https://github.com/discord-jda/JDA/blob/master/src/examples/java/SlashBotExample.java
