package tc.oc.squads;

import static tc.oc.pgm.util.text.TextException.exception;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.SquadIntegration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEndEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;

public class SquadManager implements SquadIntegration, Listener {

  // TODO: HANDLE MAX AMOUNT OF PLAYERS
  private static final int MAX_PARTY_SIZE = 3;

  private final List<Squad> squads = new ArrayList<>();

  private final Map<UUID, ScheduledFuture<?>> playerLeave = new HashMap<>();

  private final ScheduledExecutorService executor = PGM.get().getExecutor();

  public SquadManager(PGMSquads plugin) {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public boolean areInSquad(Player a, Player b) {
    Squad squad = getSquadByPlayer(a.getUniqueId());
    return squad != null && squad.containsPlayer(b.getUniqueId());
  }

  @Override
  public Collection<UUID> getSquad(Player player) {
    Squad squad = getSquadByPlayer(player.getUniqueId());
    return squad != null ? squad.getPlayers() : null;
  }

  public Squad getSquadByLeader(MatchPlayer player) {
    UUID leader = player.getId();
    return squads.stream()
        .filter(s -> Objects.equals(s.getLeader(), leader))
        .findFirst()
        .orElse(null);
  }

  public Squad getOrCreateSquadByLeader(MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    return squad != null ? squad : createSquad(leader);
  }

  public Squad getSquadByPlayer(MatchPlayer player) {
    return getSquadByPlayer(player.getId());
  }

  public Squad getSquadByPlayer(UUID uuid) {
    return squads.stream().filter(s -> s.containsPlayer(uuid)).findFirst().orElse(null);
  }

  public void schedulePlayerLeave(UUID playerId, Squad squad) {
    reschedulePlayerLeave(
        playerId, executor.schedule(() -> leaveSquad(null, playerId, squad), 60, TimeUnit.SECONDS));
  }

  private void reschedulePlayerLeave(UUID playerId, ScheduledFuture<?> future) {
    ScheduledFuture<?> prev =
        future == null ? playerLeave.remove(playerId) : playerLeave.put(playerId, future);
    if (prev != null) prev.cancel(false);
  }

  private void updateSquad(@Nullable MatchPlayer player, Squad squad) {
    if (squad.isEmpty()) squads.remove(squad);
    if (player != null) {
      Match match = player.getMatch();
      boolean isPresent = false;
      for (UUID other : squad.getPlayers()) {
        isPresent |= other == player.getId();
        match.callEvent(new NameDecorationChangeEvent(other));
      }
      // The player may have gotten removed, if that's the case, one final name update is needed
      if (!isPresent) {
        match.callEvent(new NameDecorationChangeEvent(player.getId()));
      }
    }
  }

  /** Squad command handlers * */
  public Squad createSquad(MatchPlayer leader) {
    if (getSquadByPlayer(leader) != null) throw exception("command.squad.alreadyHasSquad");

    Squad newSquad = new Squad(leader.getId());
    squads.add(newSquad);

    updateSquad(leader, newSquad);
    return newSquad;
  }

  public void leaveSquad(MatchPlayer player) {
    Squad squad = getSquadByPlayer(player);
    if (squad == null) throw exception("command.squad.notInSquad.you");
    if (squad.getLeader().equals(player.getId()))
      throw exception("command.squad.leaderCannotLeave");
    leaveSquad(player, player.getId(), squad);
  }

  public void leaveSquad(@Nullable MatchPlayer player, UUID playerId, Squad squad) {
    reschedulePlayerLeave(playerId, null);
    squad.removePlayer(playerId);
    updateSquad(player, squad);
  }

  public void kickPlayer(@Nullable MatchPlayer player, UUID uuid, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null) throw exception("command.squad.notLeader");
    if (!squad.containsPlayer(uuid)) {
      if (!squad.containsInvite(uuid)) throw exception("command.squad.notInSquad.them");
      squad.expireInvite(uuid);
    }

    leaveSquad(player, uuid, squad);
  }

  public void disband(MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null) throw exception("command.squad.notLeader");
    squads.remove(squad);
    updateSquad(leader, squad);
  }

  /** Invite command handlers * */
  public void createInvite(MatchPlayer invited, MatchPlayer leader) {
    if (getSquadByPlayer(invited) != null) throw exception("command.squad.alreadyInSquad");

    Squad squad = getOrCreateSquadByLeader(leader);
    if (squad.totalSize() >= MAX_PARTY_SIZE) throw exception("command.squad.squadIsFull");

    UUID invitedUuid = invited.getId();

    if (!squad.addInvite(invited.getId())) throw exception("command.squad.alreadyJoinedOrInvited");
    executor.schedule(() -> squad.expireInvite(invitedUuid), 30, TimeUnit.SECONDS);
  }

  public void acceptInvite(MatchPlayer player, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null || !squad.acceptInvite(player.getId()))
      throw exception("command.squad.inviteNotFound");

    updateSquad(player, squad);
    squads.forEach(s -> s.expireInvite(player.getId()));
  }

  public void expireInvite(MatchPlayer player, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null || !squad.expireInvite(player.getId()))
      throw exception("command.squad.inviteNotFound");
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    reschedulePlayerLeave(event.getPlayer().getUniqueId(), null);
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Squad squad = getSquadByPlayer(event.getPlayer().getUniqueId());
    if (squad != null) {
      schedulePlayerLeave(event.getPlayer().getUniqueId(), squad);
    }
  }

  @EventHandler
  public void onPlayerJoinMatch(MatchLoadEndEvent event) {
    JoinMatchModule jmm = event.getMatch().needModule(JoinMatchModule.class);
    squads.stream()
        .sorted(Comparator.comparingInt(s -> -s.size()))
        .forEach(
            s -> {
              List<MatchPlayer> players =
                  s.getPlayers().stream()
                      .map(uuid -> event.getMatch().getPlayer(uuid))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
              if (players.isEmpty()) return;
              MatchPlayer leader = event.getMatch().getPlayer(s.getLeader());
              if (leader == null) leader = players.get(0);
              JoinRequest request =
                  JoinRequest.group(
                      null,
                      players.size(),
                      JoinRequest.playerFlags(leader, JoinRequest.Flag.SQUAD));
              JoinResult result = jmm.queryJoin(leader, request);
              players.forEach(p -> jmm.join(p, request, result));
            });
  }
}
