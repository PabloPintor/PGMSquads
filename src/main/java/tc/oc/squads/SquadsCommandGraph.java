package tc.oc.squads;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.injectors.AudienceProvider;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.parsers.MatchPlayerParser;
import tc.oc.pgm.command.parsers.OfflinePlayerParser;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.lib.org.incendo.cloud.minecraft.extras.MinecraftHelp;
import tc.oc.pgm.util.Audience;

public class SquadsCommandGraph extends CommandGraph<PGMSquads> {

  public SquadsCommandGraph(PGMSquads plugin) throws Exception {
    super(plugin);
  }

  @Override
  protected MinecraftHelp<CommandSender> createHelp() {
    return null;
  }

  @Override
  protected void setupInjectors() {
    registerInjector(Audience.class, new AudienceProvider());
    this.registerInjector(MatchPlayer.class, new MatchPlayerProvider());
  }

  @Override
  protected void setupParsers() {
    this.registerParser(OfflinePlayer.class, new OfflinePlayerParser());
    this.registerParser(MatchPlayer.class, new MatchPlayerParser());
  }

  @Override
  protected void registerCommands() {
    register(new SquadCommand(plugin.getSquadManager()));
  }
}
