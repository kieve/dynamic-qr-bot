package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.model.QrMapping;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import ca.kieve.dynamicqrbot.service.QrImageService;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class UpdateQrCommand extends BaseAdminCommand {
    private final QrImageService m_qrImageService;

    public UpdateQrCommand(QrBotProperties properties, BotConfigService configService,
            QrImageService qrImageService) {
        super(properties, configService);
        m_qrImageService = qrImageService;
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
        if (denyIfUnauthorized(event)) {
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
            m_configService.getMappingByNickname(nickname)
                    .ifPresent(m_qrImageService::generateQrImage);
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
        autoCompleteNickname(event);
    }
}
