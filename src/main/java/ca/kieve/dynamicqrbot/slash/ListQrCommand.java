package ca.kieve.dynamicqrbot.slash;

import ca.kieve.dynamicqrbot.config.QrBotProperties;
import ca.kieve.dynamicqrbot.model.QrMapping;
import ca.kieve.dynamicqrbot.service.BotConfigService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListQrCommand implements SlashCommand {

    private final QrBotProperties m_properties;
    private final BotConfigService m_configService;

    public ListQrCommand(QrBotProperties properties, BotConfigService configService) {
        m_properties = properties;
        m_configService = configService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("list-qr", "List all QR code mappings")
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

        List<QrMapping> mappings = m_configService.getMappings();
        if (mappings.isEmpty()) {
            event.reply("No QR mappings configured.").setEphemeral(true).queue();
            return;
        }

        StringBuilder sb = new StringBuilder("**QR Mappings:**\n");
        for (QrMapping m : mappings) {
            sb.append("- **").append(m.nickname()).append("** — `/")
                    .append(m.staticPath()).append("` → ")
                    .append(m.destinationUrl()).append("\n");
        }

        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    private boolean isAuthorized(long userId) {
        return (m_properties.globalAdmin() != null && m_properties.globalAdmin() == userId)
                || m_configService.isAdmin(userId);
    }
}
