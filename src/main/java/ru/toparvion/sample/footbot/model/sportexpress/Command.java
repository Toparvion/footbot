package ru.toparvion.sample.footbot.model.sportexpress;

import lombok.Data;

import java.util.Objects;

@Data
public class Command {
  String id;
  String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Command command = (Command) o;
    return Objects.equals(id, command.id) &&
        Objects.equals(name, command.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
