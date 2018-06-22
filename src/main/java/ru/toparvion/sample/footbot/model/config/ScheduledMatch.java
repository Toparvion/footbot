package ru.toparvion.sample.footbot.model.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE)
@Getter
@Setter
@NoArgsConstructor
public class ScheduledMatch {
  String id;
  ZonedDateTime date;
  String title;

  public void setDate(String date) {
    this.date = ZonedDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yy HH:mm VV"));
  }
}
