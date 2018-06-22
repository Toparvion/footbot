package ru.toparvion.sample.footbot.model.sportexpress.event;

import lombok.Data;
import ru.toparvion.sample.footbot.model.sportexpress.Command;
import ru.toparvion.sample.footbot.model.sportexpress.Player;

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
}
