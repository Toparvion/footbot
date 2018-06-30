package ru.toparvion.sample.footbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Configuration
public class BotApiConfig {

  @Bean
  public DefaultBotOptions prepareBotOptions(@Value("${telegram.bot.proxy.host:}") String proxyHost,
                                             @Value("${telegram.bot.proxy.port:3128}") int proxyPort) {
    ApiContextInitializer.init();
    // Set up Http proxy
    DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
    if (hasText(proxyHost)) {
      HttpHost httpHost = new HttpHost(proxyHost, proxyPort);
      RequestConfig requestConfig = RequestConfig.custom()
              .setProxy(httpHost)
              .setAuthenticationEnabled(false)
              .build();
      botOptions.setRequestConfig(requestConfig);
      botOptions.setHttpProxy(httpHost);
    }
    return botOptions;
  }

}
