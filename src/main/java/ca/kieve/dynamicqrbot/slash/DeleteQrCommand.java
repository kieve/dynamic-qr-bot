package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
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
public class DeleteQrCommand extends BaseAdminCommand {
    private final QrImageService m_qrImageService;

    public DeleteQrCommand(QrBotProperties properties, BotConfigService configService,
            QrImageService qrImageService) {
        super(properties, configService);
        m_qrImageService = qrImageService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("delete-qr", "Delete a QR code mapping")
                .addOptions(new OptionData(OptionType.STRING, "nickname",
                        "The nickname of the QR mapping to delete", true)
                        .setAutoComplete(true))
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (denyIfUnauthorized(event)) {
            return;
        }

        OptionMapping nicknameOption = event.getOption("nickname");
        if (nicknameOption == null) {
            event.reply("You must specify a nickname.").setEphemeral(true).queue();
            return;
        }

        String nickname = nicknameOption.getAsString();
        boolean deleted = m_configService.deleteMapping(nickname);
        if (deleted) {
            m_qrImageService.deleteQrImage(nickname);
            event.reply("Deleted QR mapping **" + nickname + "**.")
                    .setEphemeral(true).queue();
        } else {
            event.reply("No QR mapping found with nickname **" + nickname + "**.")
                    .setEphemeral(true).queue();
        }
    }

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        autoCompleteNickname(event);
    }
}
