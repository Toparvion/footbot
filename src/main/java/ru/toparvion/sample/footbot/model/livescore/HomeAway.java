package ru.toparvion.sample.footbot.model.livescore;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Toparvion
 * @since v0.9
 */
public enum HomeAway {
  @JsonProperty("h") HOME,
  @JsonProperty("a") AWAY
}
