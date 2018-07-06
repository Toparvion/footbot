package ru.toparvion.sample.footbot.model.sportexpress;

import lombok.Data;

import java.util.Objects;

@Data
public class Player {
  String id;
  String name;
  String originName;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Player player = (Player) o;
    return Objects.equals(id, player.id) &&
        Objects.equals(name, player.name) &&
        Objects.equals(originName, player.originName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, originName);
  }
}
