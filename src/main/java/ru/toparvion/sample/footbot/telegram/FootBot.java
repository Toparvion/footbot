package ru.toparvion.sample.footbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
public class FootBot extends AbilityBot {

  private final String botUserName;
  private final String botToken;
  private final int botCreatorId;

  private BotSession botSession = null;

  public FootBot(String botUserName, String botToken, int botCreatorId, DefaultBotOptions options) {
    super(botToken, botUserName, options);
    this.botUserName = botUserName;
    this.botCreatorId = botCreatorId;
    this.botToken = botToken;
  }

  @PostConstruct
  public void startBotSession() {
    if (botSession != null) {
      return;
    }

    log.debug("Starting FootBot session...");
    try {
      TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
      botSession = telegramBotsApi.registerBot(this);
      log.info("FootBot session started.");

    } catch (TelegramApiException e) {
      log.error("Failed to register FootBot with Telegram API", e);
    }
  }

  @PreDestroy
  public void stopBotSession() {
    log.debug("FootBot session is about to stop...");
    botSession.stop();
    log.info("FootBot session stopped.");
  }

  public Ability sayHelloWorld() {
    return Ability
            .builder()
            .name("hello")
            .info("says hello world!")
            .locality(ALL)
            .privacy(PUBLIC)
            .action(ctx -> silent.send("Hello world!", ctx.chatId()))
            .build();
  }

  @Override
  public String getBotUsername() {
    return botUserName;
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public int creatorId() {
    return botCreatorId;
  }
}
