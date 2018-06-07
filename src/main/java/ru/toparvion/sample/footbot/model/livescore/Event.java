package ru.toparvion.sample.footbot.model.livescore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * {@code
 * {
 *     "id": "820075",
 *     "match_id": "93336",
 *     "player": "CANGA RUBEN",
 *     "time": "64",
 *     "event": "RED_CARD",
 *     "sort": "0",
 *     "home_away": "a"
 * }}
 * @author Toparvion
 * @since v0.9
 *
 */
@Data
public class Event {
  String id;
  @JsonProperty("match_id")
  String matchId;
  String player;
  int time;
  EventType event;
  int sort;
  @JsonProperty("home_away")
  HomeAway homeAway;
}
