package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

import java.util.List;

public abstract class BaseAdminCommand implements SlashCommand {

    protected final QrBotProperties m_properties;
    protected final BotConfigService m_configService;

    protected BaseAdminCommand(QrBotProperties properties, BotConfigService configService) {
        m_properties = properties;
        m_configService = configService;
    }

    protected boolean isAuthorized(long userId) {
        return isGlobalAdmin(userId) || m_configService.isAdmin(userId);
    }

    protected boolean isGlobalAdmin(long userId) {
        return m_properties.globalAdmin() != null
                && m_properties.globalAdmin() == userId;
    }

    protected boolean denyIfUnauthorized(SlashCommandInteractionEvent event) {
        if (!isAuthorized(event.getUser().getIdLong())) {
            event.reply("You do not have permission to use this command.")
                    .setEphemeral(true).queue();
            return true;
        }
        return false;
    }

    protected void autoCompleteNickname(CommandAutoCompleteInteractionEvent event) {
        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Choice> choices = m_configService.getNicknames().stream()
                .filter(name -> name.toLowerCase().contains(input))
                .limit(25)
                .map(name -> new Choice(name, name))
                .toList();
        event.replyChoices(choices).queue();
    }
}
