package tc.oc.squads;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@CommandMethod("party|squad|p")
public class SquadCommand {

  private final SquadManager manager;

  public SquadCommand(SquadManager manager) {
    this.manager = manager;
  }

  @CommandMethod("")
  @CommandDescription("List party members")
  @CommandPermission(Permissions.SQUAD_CREATE)
  public void listDefault(MatchPlayer sender, CommandSender cmdSend) {
    list(sender, cmdSend);
  }

  @CommandMethod("<player>")
  @CommandDescription("Send a squad invitation")
  @CommandPermission(Permissions.SQUAD_CREATE)
  public void directInvite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    invite(sender, invited);
  }

  @CommandMethod("create")
  @CommandDescription("Create a squad")
  @CommandPermission(Permissions.SQUAD_CREATE)
  public void create(MatchPlayer sender) {
    manager.createSquad(sender);
    sender.sendMessage(text("Created a new party"));
  }

  @CommandMethod("invite <player>")
  @CommandDescription("Send a squad invitation")
  @CommandPermission(Permissions.SQUAD_CREATE)
  public void invite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    manager.createInvite(invited, sender);

    sender.sendMessage(text("Party invite sent"));

    String leaderName = Players.getVisibleName(invited.getBukkit(), sender.getBukkit());

    invited.sendMessage(
        text()
            .append(text("You were invited to ", NamedTextColor.YELLOW))
            .append(sender.getName())
            .append(text("'s party. ", NamedTextColor.YELLOW))
            .append(
                text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/party accept " + leaderName)))
            .append(text(" "))
            .append(
                text("[DENY]", NamedTextColor.RED, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/party deny " + leaderName))));
  }

  @CommandMethod("accept <player>")
  @CommandDescription("Accept a squad invitation")
  @CommandPermission(Permissions.SQUAD)
  public void accept(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.acceptInvite(sender, leader);

    leader.sendMessage(
        text().append(sender.getName()).append(text(" has accepted your invitation")));
    sender.sendMessage(text("Joined the party"));
  }

  @CommandMethod("deny <player>")
  @CommandDescription("Deny a squad invitation")
  @CommandPermission(Permissions.SQUAD)
  public void deny(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.expireInvite(sender, leader);

    leader.sendMessage(text().append(sender.getName()).append(text(" has denied your invitation")));
    sender.sendMessage(text("Invitation denied"));
  }

  @CommandMethod("leave")
  @CommandDescription("Leave your current party")
  @CommandPermission(Permissions.SQUAD)
  public void leave(MatchPlayer sender) {
    manager.leaveSquad(sender);
    sender.sendMessage(text("Left the party"));
  }

  @CommandMethod("list")
  @CommandDescription("List party members")
  @CommandPermission(Permissions.SQUAD)
  public void list(MatchPlayer sender, CommandSender cmdSend) {
    Squad squad = manager.getSquadByPlayer(sender);
    if (squad == null) throw exception("commands.squad.notInSquad");

    boolean isLeader = Objects.equals(sender.getId(), squad.getLeader());

    Component header =
        TextFormatter.horizontalLineHeading(
            cmdSend,
            text().append(player(squad.getLeader(), NameStyle.VERBOSE)).append(text("'s Party")),
            NamedTextColor.BLUE);

    new PrettyPaginatedComponentResults<UUID>(header, squad.totalSize()) {
      @Override
      public Component format(UUID player, int index) {
        TextComponent.Builder builder =
            text()
                .append(text(index + 1))
                .append(text(". "))
                .append(player(player, NameStyle.FANCY));
        if (squad.getLeader().equals(player)) {
          builder.append(text(" (leader)"));
        } else {
          if (index >= squad.size()) {
            builder.append(text(" (pending) ", NamedTextColor.GRAY, TextDecoration.ITALIC));
          }
          if (isLeader) {
            builder.append(
                text(" \u2715", NamedTextColor.DARK_RED)
                    .hoverEvent(showText(text("Remove player from party")))
                    .clickEvent(runCommand("/party kick " + player)));
          }
        }
        return builder.build();
      }
    }.display(sender, ImmutableList.copyOf(squad.getAllPlayers()), 1);
  }

  @CommandMethod("kick <player>")
  @CommandDescription("Kick a player from your party")
  @CommandPermission(Permissions.SQUAD)
  public void kick(MatchPlayer sender, @Argument("player") OfflinePlayer player) {
    MatchPlayer target = PGM.get().getMatchManager().getPlayer(player.getUniqueId());
    manager.kickPlayer(target, player.getUniqueId(), sender);
    sender.sendMessage(text("Kicked player from your party"));
    if (target != null)
      target.sendMessage(
          text()
              .append(text("You were kicked from ", NamedTextColor.RED))
              .append(sender.getName())
              .append(text("'s party", NamedTextColor.RED)));
  }

  @CommandMethod("disband")
  @CommandDescription("Disband your current party")
  @CommandPermission(Permissions.SQUAD)
  public void disband(MatchPlayer sender) {
    manager.disband(sender);
    sender.sendMessage(text("Disbanded your party"));
  }
}
