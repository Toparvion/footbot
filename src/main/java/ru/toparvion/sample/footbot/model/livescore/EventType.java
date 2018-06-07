package ru.toparvion.sample.footbot.model.livescore;

/**
 * <pre><code>
 * GOAL	A goal was scored
 * GOAL_PENALTY	A goal was scored from a penalty
 * OWN_GOAL	An own goal was scored
 * YELLOW_CARD	A player has been cautioned by the referee
 * RED_CARD	A player was sent off by the referee
 * YELLOW_RED_CARD	A player has received their second yellow card and they have been sent off.
 * </code></pre>
 *
 * @author Toparvion
 * @since v0.9
 */
public enum EventType {
  GOAL,
  GOAL_PENALTY,
  OWN_GOAL,
  YELLOW_CARD,
  RED_CARD,
  YELLOW_RED_CARD
}
