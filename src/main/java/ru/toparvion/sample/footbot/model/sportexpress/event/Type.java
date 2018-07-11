package ru.toparvion.sample.footbot.model.sportexpress.event;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Type {
  @JsonEnumDefaultValue
  text,
  change,
  card,
  statechange,
  goal,
  none /* artificial */,
  // additional, non-selectable types
  penaltynogoal,
  penaltyseriegoal,
  penaltyserienogoal
}
