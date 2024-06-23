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
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@Command("party|squad|p")
public class SquadCommand {

  private final SquadManager manager;

  public SquadCommand(SquadManager manager) {
    this.manager = manager;
  }

  @Command("")
  @CommandDescription("List party members")
  @Permission(Permissions.SQUAD)
  public void listDefault(MatchPlayer sender) {
    list(sender);
  }

  @Command("<player>")
  @CommandDescription("Send a squad invitation")
  @Permission(Permissions.SQUAD_CREATE)
  public void directInvite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    invite(sender, invited);
  }

  @Command("create")
  @CommandDescription("Create a squad")
  @Permission(Permissions.SQUAD_CREATE)
  public void create(MatchPlayer sender) {
    manager.createSquad(sender);
    sender.sendMessage(translatable("squad.create.success", NamedTextColor.YELLOW));
  }

  @Command("invite <player>")
  @CommandDescription("Send a squad invitation")
  @Permission(Permissions.SQUAD_CREATE)
  public void invite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    manager.createInvite(invited, sender);

    sender.sendMessage(translatable(
        "squad.invite.sent", NamedTextColor.YELLOW, invited.getName(NameStyle.VERBOSE)));

    String leaderName = Players.getVisibleName(invited.getBukkit(), sender.getBukkit());

    invited.sendMessage(translatable(
            "squad.invite.received",
            NamedTextColor.YELLOW,
            sender.getName(NameStyle.VERBOSE),
            translatable("squad.invite.accept", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party accept " + leaderName)),
            translatable("squad.invite.deny", NamedTextColor.RED, TextDecoration.BOLD))
        .clickEvent(ClickEvent.runCommand("/party deny " + leaderName)));
  }

  @Command("accept <player>")
  @CommandDescription("Accept a squad invitation")
  @Permission(Permissions.SQUAD)
  public void accept(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.acceptInvite(sender, leader);

    leader.sendMessage(translatable(
        "squad.accept.leader", NamedTextColor.GREEN, sender.getName(NameStyle.VERBOSE)));
    sender.sendMessage(translatable(
        "squad.accept.invited", NamedTextColor.GREEN, leader.getName(NameStyle.VERBOSE)));
  }

  @Command("deny <player>")
  @CommandDescription("Deny a squad invitation")
  @Permission(Permissions.SQUAD)
  public void deny(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    manager.expireInvite(sender, leader);

    leader.sendMessage(
        translatable("squad.deny.leader", NamedTextColor.RED, sender.getName(NameStyle.VERBOSE)));
    sender.sendMessage(
        translatable("squad.deny.invited", NamedTextColor.RED, leader.getName(NameStyle.VERBOSE)));
  }

  @Command("leave")
  @CommandDescription("Leave your current party")
  @Permission(Permissions.SQUAD)
  public void leave(MatchPlayer sender) {
    manager.leaveSquad(sender);
    sender.sendMessage(translatable("squad.leave.success", NamedTextColor.YELLOW));
  }

  @Command("list")
  @CommandDescription("List party members")
  @Permission(Permissions.SQUAD)
  public void list(MatchPlayer sender) {
    Squad squad = manager.getSquadByPlayer(sender);
    if (squad == null) throw exception("squad.err.memberOnly");

    boolean isLeader = Objects.equals(sender.getId(), squad.getLeader());

    Component header = TextFormatter.horizontalLineHeading(
        sender.getBukkit(),
        translatable("squad.list.header", player(squad.getLeader(), NameStyle.VERBOSE)),
        NamedTextColor.BLUE);

    new PrettyPaginatedComponentResults<UUID>(header, squad.totalSize()) {
      @Override
      public Component format(UUID player, int index) {
        TextComponent.Builder builder = text()
            .append(text(index + 1))
            .append(text(". "))
            .append(player(player, NameStyle.VERBOSE));
        if (squad.getLeader().equals(player)) {
          builder
              .append(text(" "))
              .append(
                  translatable("squad.list.leader", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
          if (index >= squad.size()) {
            builder
                .append(text(" "))
                .append(
                    translatable("squad.list.pending", NamedTextColor.GRAY, TextDecoration.ITALIC));
          }
          if (isLeader) {
            builder
                .append(text(" "))
                .append(text("\u2715", NamedTextColor.DARK_RED)
                    .hoverEvent(
                        showText(translatable("squad.list.removeHover", NamedTextColor.RED)))
                    .clickEvent(runCommand("/party kick " + player)));
          }
        }
        return builder.build();
      }
    }.display(sender, ImmutableList.copyOf(squad.getAllPlayers()), 1);
  }

  @Command("kick <player>")
  @CommandDescription("Kick a player from your party")
  @Permission(Permissions.SQUAD)
  public void kick(MatchPlayer sender, @Argument("player") OfflinePlayer player) {
    MatchPlayer target = PGM.get().getMatchManager().getPlayer(player.getUniqueId());
    manager.kickPlayer(target, player.getUniqueId(), sender);
    sender.sendMessage(translatable(
        "squad.kicked.leader",
        NamedTextColor.YELLOW,
        player(player.getUniqueId(), NameStyle.VERBOSE)));
    if (target != null)
      target.sendMessage(translatable("squad.kicked.player", NamedTextColor.RED, sender.getName()));
  }

  @Command("disband")
  @CommandDescription("Disband your current party")
  @Permission(Permissions.SQUAD)
  public void disband(MatchPlayer sender) {
    manager.disband(sender);
    sender.sendMessage(translatable("squad.disband.success", NamedTextColor.YELLOW));
  }
}
