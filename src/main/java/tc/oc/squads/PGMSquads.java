package tc.oc.squads;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.command.util.PGMCommandGraph;
import tc.oc.squads.text.TextTranslations;

public class PGMSquads extends JavaPlugin {

  @Override
  public void onEnable() {
    // Force translations to load
    TextTranslations.load();

    SquadManager squadManager = new SquadManager(this);
    try {
      new PGMCommandGraph(PGM.get()) {
        @Override
        protected void registerCommands() {
          register(new SquadCommand(squadManager));
        }
      };
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Exception registering commands", e);
    }
    Integration.setSquadIntegration(squadManager);

    getLogger().info("Squads have been enabled");
  }
}
