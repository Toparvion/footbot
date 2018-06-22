package ru.toparvion.sample.footbot.model.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "sportexpress")
public class Schedule {
  private List<ScheduledMatch> schedule = new ArrayList<>();
}
