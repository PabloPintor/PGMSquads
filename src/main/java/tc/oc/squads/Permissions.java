package tc.oc.squads;

/** This is a hard-coded list of permissions, that add the basic functionality. */
public interface Permissions {

  // Root permission node
  String ROOT = "squads";

  // Individual permission nodes
  String SQUAD = ROOT + ".squad"; // Root command for squads
  String SQUAD_CREATE = SQUAD + ".create"; // Can create a squad
}
