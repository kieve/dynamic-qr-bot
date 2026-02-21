package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.model.QrMapping;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import ca.kieve.dynamicqrbot.service.QrImageService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

@Component
public class AddQrCommand extends BaseAdminCommand {
    private final QrImageService m_qrImageService;

    public AddQrCommand(QrBotProperties properties, BotConfigService configService,
            QrImageService qrImageService) {
        super(properties, configService);
        m_qrImageService = qrImageService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("add-qr", "Add a new QR code mapping")
                .addOption(OptionType.STRING, "nickname", "A nickname for the QR mapping", true)
                .addOption(OptionType.STRING, "path", "The static path (e.g. my-link)", true)
                .addOption(OptionType.STRING, "url", "The destination URL", true)
                .setIntegrationTypes(IntegrationType.ALL)
                .setContexts(InteractionContextType.ALL);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (denyIfUnauthorized(event)) {
            return;
        }

        OptionMapping nicknameOption = event.getOption("nickname");
        OptionMapping pathOption = event.getOption("path");
        OptionMapping urlOption = event.getOption("url");

        if (nicknameOption == null || pathOption == null || urlOption == null) {
            event.reply("All options (nickname, path, url) are required.")
                    .setEphemeral(true).queue();
            return;
        }

        String nickname = nicknameOption.getAsString();
        String path = pathOption.getAsString();
        String url = urlOption.getAsString();

        boolean added = m_configService.addMapping(nickname, path, url);
        if (added) {
            m_qrImageService.generateQrImage(new QrMapping(nickname, path, url));
            event.reply("Added new QR mapping **" + nickname + "** — `/"
                            + path + "` → " + url)
                    .setEphemeral(true).queue();
        } else {
            event.reply("A mapping with that nickname or path already exists.")
                    .setEphemeral(true).queue();
        }
    }
}
