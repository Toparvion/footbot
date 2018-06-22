package ru.toparvion.sample.footbot.model.sportexpress;

import lombok.Data;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;

import java.util.List;

@Data
public class Match {
  String id;
  String date;
  String score;
  String statusName;

  Command homeCommand;
  Command guestCommand;

  String description;

  List<Event> events;
}
