package ru.toparvion.sample.footbot.model.sportexpress.event;

public enum Type {
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
