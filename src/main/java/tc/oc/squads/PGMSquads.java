package tc.oc.squads;

import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.command.util.PGMCommandGraph;

public class PGMSquads extends JavaPlugin {

  @Override
  public void onEnable() {
    SquadManager squadManager = new SquadManager(this);
    try {
      new PGMCommandGraph(PGM.get()) {
        @Override
        protected void registerCommands() {
          register(new SquadCommand(squadManager));
        }
      };
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Integration.setSquadIntegration(squadManager);
  }
}
