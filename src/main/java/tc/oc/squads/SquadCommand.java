package tc.oc.squads;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
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
    sender.sendMessage(translatable("command.squad.partyCreated"));
  }

  @CommandMethod("invite <player>")
  @CommandDescription("Send a squad invitation")
  @CommandPermission(Permissions.SQUAD_CREATE)
  public void invite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    manager.createInvite(invited, sender);

    sender.sendMessage(translatable("command.squad.inviteSent"));

    String leaderName = Players.getVisibleName(invited.getBukkit(), sender.getBukkit());

    invited.sendMessage(
        translatable(
                "command.squad.wasInvited",
                sender.getName(),
                translatable("command.squad.accept", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/party accept " + leaderName)),
                translatable("command.squad.deny", NamedTextColor.RED, TextDecoration.BOLD))
            .clickEvent(ClickEvent.runCommand("/party deny " + leaderName)));
  }

  @CommandMethod("accept <player>")
  @CommandDescription("Accept a squad invitation")
  @CommandPermission(Permissions.SQUAD)
  public void accept(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.acceptInvite(sender, leader);

    leader.sendMessage(translatable("command.squad.inviteAccepted.leader", sender.getName()));
    sender.sendMessage(translatable("command.squad.inviteAccepted.invited"));
  }

  @CommandMethod("deny <player>")
  @CommandDescription("Deny a squad invitation")
  @CommandPermission(Permissions.SQUAD)
  public void deny(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.expireInvite(sender, leader);

    leader.sendMessage(translatable("command.squad.inviteDenied.leader", sender.getName()));
    sender.sendMessage(translatable("command.squad.inviteDenied.invited"));
  }

  @CommandMethod("leave")
  @CommandDescription("Leave your current party")
  @CommandPermission(Permissions.SQUAD)
  public void leave(MatchPlayer sender) {
    manager.leaveSquad(sender);
    sender.sendMessage(translatable("command.squad.partyLeft"));
  }

  @CommandMethod("list")
  @CommandDescription("List party members")
  @CommandPermission(Permissions.SQUAD)
  public void list(MatchPlayer sender, CommandSender cmdSend) {
    Squad squad = manager.getSquadByPlayer(sender);
    if (squad == null) throw exception("command.squad.notInSquad.you");

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
          builder.append(text(" ").append(translatable("command.squad.list.leader")));
        } else {
          if (index >= squad.size()) {
            builder.append(text(" "));
            builder.append(
                translatable(
                    "command.squad.list.pending", NamedTextColor.GRAY, TextDecoration.ITALIC));
          }
          if (isLeader) {
            builder.append(
                text(" \u2715", NamedTextColor.DARK_RED)
                    .hoverEvent(showText(translatable("command.squad.removePlayer")))
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
    sender.sendMessage(translatable("command.squad.playerKicked"));
    if (target != null)
      target.sendMessage(
          translatable("command.squad.wasKicked", NamedTextColor.RED, sender.getName()));
  }

  @CommandMethod("disband")
  @CommandDescription("Disband your current party")
  @CommandPermission(Permissions.SQUAD)
  public void disband(MatchPlayer sender) {
    manager.disband(sender);
    sender.sendMessage(translatable("command.squad.partyDisbanded"));
  }
}
