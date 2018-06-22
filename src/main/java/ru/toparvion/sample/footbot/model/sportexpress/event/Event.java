package ru.toparvion.sample.footbot.model.sportexpress.event;

import lombok.Data;
import ru.toparvion.sample.footbot.model.sportexpress.Command;

import java.util.Objects;

@Data
public class Event {
  String id;
  Type type;
  String minute;
  Kind kind;
  String text;
  String changedStateName;

  Player player;
  Command command;
  Info info;

  Player playerIn;
  Player playerOut;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Event event = (Event) o;
    return Objects.equals(id, event.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
