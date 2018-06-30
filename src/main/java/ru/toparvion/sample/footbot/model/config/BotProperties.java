package ru.toparvion.sample.footbot.model.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@Component
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "telegram.bot")
public class BotProperties {
  private String name;
  private String token;
  private int creatorId;
}
