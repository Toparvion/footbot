package ru.toparvion.sample.footbot.model.sportexpress.event;

import lombok.Data;

import java.util.Objects;

@Data
public class Info {
  String score;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Info info = (Info) o;
    return Objects.equals(score, info.score);
  }

  @Override
  public int hashCode() {
    return Objects.hash(score);
  }
}
