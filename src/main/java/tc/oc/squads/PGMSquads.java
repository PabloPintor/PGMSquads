package tc.oc.squads;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.squads.text.TextTranslations;

public class PGMSquads extends JavaPlugin {

  private SquadManager squadManager;

  public SquadManager getSquadManager() {
    return squadManager;
  }

  @Override
  public void onEnable() {
    // Force translations to load
    TextTranslations.load();

    // Set-up manager
    this.squadManager = new SquadManager(this);

    // Set-up commands
    try {
      new SquadsCommandGraph(this);
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Exception registering commands", e);
    }

    // Integrate with PGM
    Integration.setSquadIntegration(squadManager);

    getLogger().info("Squads have been enabled");
  }

}
