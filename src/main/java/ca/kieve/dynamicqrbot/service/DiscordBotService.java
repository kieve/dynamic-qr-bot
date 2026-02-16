package ca.kieve.dynamicqrbot.service;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiscordBotService extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DiscordBotService.class);
    private static final String COMMAND_UPDATE_QR = "update-qr";
    private static final String COMMAND_ADD_TO_WHITELIST = "add-to-whitelist";

    private final QrBotProperties m_properties;
    private final BotConfigService m_configService;
    private JDA m_jda;

    public DiscordBotService(QrBotProperties properties, BotConfigService configService) {
        this.m_properties = properties;
        this.m_configService = configService;
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

        var updateQr = Commands.slash(COMMAND_UPDATE_QR, "Update the destination URL for a QR code")
                .addOptions(new OptionData(OptionType.STRING, "nickname", "The nickname of the QR mapping", true)
                        .setAutoComplete(true))
                .addOption(OptionType.STRING, "url", "The new destination URL", true)
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);

        var addToWhitelist = Commands.slash(COMMAND_ADD_TO_WHITELIST, "Add a user to the admin whitelist")
                .addOption(OptionType.USER, "user", "The user to add to the whitelist", true)
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);

        m_jda.updateCommands().addCommands(updateQr, addToWhitelist).queue();
        LOG.info("Registered global commands: /{}, /{}", COMMAND_UPDATE_QR, COMMAND_ADD_TO_WHITELIST);

        LOG.info("Discord bot started successfully");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case COMMAND_UPDATE_QR -> handleUpdateQr(event);
            case COMMAND_ADD_TO_WHITELIST -> handleAddToWhitelist(event);
        }
    }

    private boolean isAuthorized(long userId) {
        return isGlobalAdmin(userId) || m_configService.isAdmin(userId);
    }

    private boolean isGlobalAdmin(long userId) {
        return m_properties.globalAdmin() != null
                && m_properties.globalAdmin() == userId;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(COMMAND_UPDATE_QR)) {
            return;
        }

        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Choice> choices = m_configService.getNicknames().stream()
                .filter(name -> name.toLowerCase().contains(input))
                .limit(25)
                .map(name -> new Choice(name, name))
                .toList();
        event.replyChoices(choices).queue();
    }

    private void handleUpdateQr(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (!isAuthorized(userId)) {
            event.reply("You do not have permission to use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        OptionMapping nicknameOption = event.getOption("nickname");
        OptionMapping urlOption = event.getOption("url");

        if (nicknameOption == null || urlOption == null) {
            event.reply("Both nickname and url are required.").setEphemeral(true).queue();
            return;
        }

        String nickname = nicknameOption.getAsString();
        String url = urlOption.getAsString();

        boolean updated = m_configService.updateDestination(nickname, url);
        if (updated) {
            event.reply("Updated **" + nickname + "** to point to: " + url)
                    .setEphemeral(true)
                    .queue();
        } else {
            event.reply("No QR mapping found with nickname **" + nickname + "**.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void handleAddToWhitelist(SlashCommandInteractionEvent event) {
        long callerId = event.getUser().getIdLong();
        if (!isGlobalAdmin(callerId)) {
            event.reply("Only the global admin can use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        if (userOption == null) {
            event.reply("You must specify a user.").setEphemeral(true).queue();
            return;
        }

        User targetUser = userOption.getAsUser();
        long targetUserId = targetUser.getIdLong();

        boolean added = m_configService.addAdmin(targetUserId);
        if (added) {
            event.reply("Added **" + targetUser.getAsTag() + "** to the admin whitelist.")
                    .setEphemeral(true).queue();
        } else {
            event.reply("**" + targetUser.getAsTag() + "** is already on the admin whitelist.")
                    .setEphemeral(true).queue();
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
