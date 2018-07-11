package ru.toparvion.sample.footbot.model.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;

@Component
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "sportexpress")
public class Schedule {
  private String broadcastUri;
  @DurationUnit(SECONDS)
  private Duration pollPeriod = Duration.ofSeconds(30);
  private List<ScheduledMatch> schedule = new ArrayList<>();
}
