package ca.kieve.dynamicqrbot.service;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.slash.SlashCommand;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiscordBotService extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DiscordBotService.class);

    private final QrBotProperties m_properties;
    private final Map<String, SlashCommand> m_commands;
    private JDA m_jda;

    public DiscordBotService(QrBotProperties properties, List<SlashCommand> commands) {
        m_properties = properties;
        m_commands = commands.stream()
                .collect(Collectors.toMap(
                        cmd -> cmd.getCommandData().getName(),
                        Function.identity()));
    }

    @PostConstruct
    public void start() throws Exception {
        if (m_properties.discordToken() == null || m_properties.discordToken().isBlank()) {
            LOG.warn("Discord token not configured; bot will not start");
            return;
        }

        m_jda = JDABuilder.createLight(m_properties.discordToken(), GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(this)
                .build()
                .awaitReady();

        List<SlashCommandData> commandData = m_commands.values().stream()
                .map(SlashCommand::getCommandData)
                .toList();

        m_jda.updateCommands().addCommands(commandData).queue();
        LOG.info("Registered {} global command(s): {}", commandData.size(),
                commandData.stream().map(c -> "/" + c.getName()).collect(Collectors.joining(", ")));

        LOG.info("Discord bot started successfully");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand command = m_commands.get(event.getName());
        if (command != null) {
            command.handle(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = m_commands.get(event.getName());
        if (command != null) {
            command.handleAutoComplete(event);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (m_jda != null) {
            m_jda.shutdown();
            LOG.info("Discord bot shut down");
        }
    }
}
