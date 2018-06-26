package ru.toparvion.sample.footbot.model.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotUser {
  int userId;
  String userName;
  String level;

  public Type getLevelAsType() {
    return Type.valueOf(level);
  }
}
