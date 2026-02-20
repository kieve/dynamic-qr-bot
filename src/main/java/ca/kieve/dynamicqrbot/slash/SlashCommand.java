package ca.kieve.dynamicqrbot.slash;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Interface for Discord slash commands. Implementations are auto-discovered
 * by Spring and registered with JDA at startup.
 */
public interface SlashCommand {

    /** Build the command definition (name, description, options). */
    SlashCommandData getCommandData();

    /** Handle an invocation of this command. */
    void handle(SlashCommandInteractionEvent event);

    /** Handle auto-complete for this command. Override if the command has auto-complete options. */
    default void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // no-op by default
    }
}
