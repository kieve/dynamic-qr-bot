package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class AddToWhitelistCommand implements SlashCommand {

    private final QrBotProperties m_properties;
    private final BotConfigService m_configService;

    public AddToWhitelistCommand(QrBotProperties properties, BotConfigService configService) {
        m_properties = properties;
        m_configService = configService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("add-to-whitelist", "Add a user to the admin whitelist")
                .addOption(OptionType.USER, "user", "The user to add to the whitelist", true)
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
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

    private boolean isGlobalAdmin(long userId) {
        return m_properties.globalAdmin() != null
                && m_properties.globalAdmin() == userId;
    }
}
