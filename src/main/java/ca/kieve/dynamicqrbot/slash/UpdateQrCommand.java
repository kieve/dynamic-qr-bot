package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateQrCommand implements SlashCommand {

    private final QrBotProperties m_properties;
    private final BotConfigService m_configService;

    public UpdateQrCommand(QrBotProperties properties, BotConfigService configService) {
        m_properties = properties;
        m_configService = configService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("update-qr", "Update the destination URL for a QR code")
                .addOptions(new OptionData(OptionType.STRING, "nickname",
                        "The nickname of the QR mapping", true)
                        .setAutoComplete(true))
                .addOption(OptionType.STRING, "url", "The new destination URL", true)
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
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

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Choice> choices = m_configService.getNicknames().stream()
                .filter(name -> name.toLowerCase().contains(input))
                .limit(25)
                .map(name -> new Choice(name, name))
                .toList();
        event.replyChoices(choices).queue();
    }

    private boolean isAuthorized(long userId) {
        return (m_properties.globalAdmin() != null && m_properties.globalAdmin() == userId)
                || m_configService.isAdmin(userId);
    }
}
