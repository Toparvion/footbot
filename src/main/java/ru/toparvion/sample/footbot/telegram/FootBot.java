package ru.toparvion.sample.footbot.telegram;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.generics.BotSession;
import ru.toparvion.sample.footbot.dao.UserDao;
import ru.toparvion.sample.footbot.flow.BroadcastFlowConfig;
import ru.toparvion.sample.footbot.model.config.BotProperties;
import ru.toparvion.sample.footbot.model.db.BotUser;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.integration.support.MessageBuilder.withPayload;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.*;

@Slf4j
@Service
public class FootBot extends AbilityBot {

  private final BotProperties botProperties;
  private final BroadcastFlowConfig flowConfig;
  private final InteractionHelper helper;
  private final UserDao userDao;

  private BotSession botSession = null;

  @Autowired
  public FootBot(BotProperties botProperties,
                 DefaultBotOptions options,
                 UserDao userDao,
                 BroadcastFlowConfig flowConfig,
                 InteractionHelper helper) {
    super(botProperties.getToken(), botProperties.getName(), options);
    this.botProperties = botProperties;
    this.userDao = userDao;
    this.flowConfig = flowConfig;
    this.helper = helper;
  }

  @PostConstruct
  public void startBotSession() {
    if (!botProperties.isEnabled()) {
      log.info("FootBot отключен.");
      return;
    }
    if (botSession != null) {
      log.warn("FootBot сессия уже открыта. Пропускаю.");
      return;
    }
    log.debug("Открываю FootBot сессию...");
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
      botSession = telegramBotsApi.registerBot(this);
      stopwatch.stop();
      log.info("FootBot сессия открыта за {}.", stopwatch);

    } catch (TelegramApiException e) {
      stopwatch.stop();
      log.error("Failed to register FootBot with Telegram API", e);
    }
  }

  @PreDestroy
  public void stopBotSession() {
    if (botSession == null) {
      log.info("FootBot сессия не открыта. Пропускаю закрытие.");
      return;
    }
    log.debug("Закрываю FootBot сессию...");
    Stopwatch stopwatch = Stopwatch.createStarted();
    botSession.stop();
    stopwatch.stop();
    log.info("FootBot сессия закрыта за {}.", stopwatch);
  }

  @SuppressWarnings("unused")
  public Ability listLevelsAbility() {
    return Ability
            .builder()
            .name("start")
            .info("Настроить получение оповещений")
            .locality(USER)
            .privacy(PUBLIC)
            .action(this::listLevels)
            .build();
  }

  @SuppressWarnings("unused")
  public Reply reactOnSelectReply() {
    return Reply.of(this::registerUser, Flag.CALLBACK_QUERY);
  }

  @SuppressWarnings("unused")
  public Ability helpAbility() {
    return Ability
        .builder()
        .name("help")
        .info("справка")
        .locality(USER)
        .privacy(PUBLIC)
        .action(this::sendHelpText)
        .build();
  }

  private void registerUser(Update update) {
    CallbackQuery callbackQuery = update.getCallbackQuery();
    Type chosenLevel = Type.valueOf(callbackQuery.getData());
    Long chatId = AbilityUtils.getChatId(update);
    User user = AbilityUtils.getUser(update);
    BotUser botUser = new BotUser(user.getId(), helper.composeUserName(user), chosenLevel.name());
    log.info("Сформирован пользовательский выбор: {}", botUser);
    userDao.saveUser(botUser);
    flowConfig.startUserFlow(user.getId(), chosenLevel, this::sendEventViaBot);

    Message originMessage = callbackQuery.getMessage();
    String answerText = helper.composeLevelSelectAnswer(chosenLevel);
    if (originMessage != null) {
      EditMessageText editMessage = new EditMessageText();
      editMessage.setMessageId(originMessage.getMessageId());
      editMessage.enableMarkdown(true);
      editMessage.setChatId(chatId);
      editMessage.setText(answerText);
      silent.execute(editMessage);
    } else {
      silent.sendMd(answerText, chatId);
    }

    String creatorNotification = helper.composeCreatorUserRegistrationNotification(botUser);
    silent.sendMd(creatorNotification, creatorId());
  }

  private org.springframework.messaging.Message<Message> sendEventViaBot(Event event, Map<String, Object> metaData) {
    Integer userId = (Integer) metaData.get(USER_ID_HEADER);
    try {
      Long chatId = Long.valueOf(userId);
      Integer editableMessageId = (Integer) metaData.get(EDITABLE_MESSAGE_ID_HEADER);
      String messageText = helper.composeEventText(event, metaData);
      Message sentTelegramMessage;

      if (editableMessageId != null) {
        log.debug("Редактирую сообщение {} для пользователя {}: {}", editableMessageId, userId, messageText);
        EditMessageText editMessage = new EditMessageText();
        editMessage.setMessageId(editableMessageId);
        editMessage.enableMarkdown(true);
        editMessage.setChatId(chatId);
        editMessage.setText(messageText);
        sentTelegramMessage = (Message) sender.execute(editMessage);

      } else {
        SendMessage sendMessage = new SendMessage(chatId, messageText);
        sendMessage.enableMarkdown(true);
        sentTelegramMessage = sender.execute(sendMessage);
      }

      Integer messageId = sentTelegramMessage.getMessageId();
      log.debug("Отправлено сообщение {} пользователю {}: {}", messageId, userId, sentTelegramMessage.getText());
      return withPayload(sentTelegramMessage)
          .copyHeaders(metaData)
          .setHeader(EVENT_ID_HEADER, event.getId())
          .build();

    } catch (TelegramApiRequestException e) {
      handleTgApiException(e, userId, event.getId());
      return null;

    } catch (TelegramApiException e) {
      log.error(format("Не удалось отправить событие %s пользователю %s", event.getId(), userId), e);
      return null;
    }
  }

  private void listLevels(MessageContext ctx) {
    SendMessage sendMessageCommand = new SendMessage();
    sendMessageCommand.setChatId(ctx.chatId());
    Optional<Type> chosenType = userDao.fetchUserLevel(ctx.chatId());
    helper.composeSelectMarkup(sendMessageCommand, chosenType.orElse(null));
    silent.execute(sendMessageCommand);
    log.info("Отправлены варианты выбора.");
  }

  private void sendHelpText(MessageContext ctx) {
    Long chatId = ctx.chatId();
    log.debug("Формирую справку для чата {}...", chatId);
    String helpText = helper.composeHelpText();
    silent.sendMd(helpText, chatId);
    log.info("Отправлена справка в чат {}.", chatId);
  }

  private void handleTgApiException(TelegramApiRequestException telegramException, Integer userId, String eventId) {
    try {
      log.error("Не удалось отправить событие {} пользователю {}: {}", eventId, userId, telegramException.toString());
      switch (telegramException.getErrorCode()) {
        case HttpStatus.SC_FORBIDDEN:
          log.warn("Пользователь {} заблокировал бота. Выставляю ему уровень подписки 'none'.");
          userDao.updateUserLevel(userId, Type.none);
          silent.sendMd(helper.composeCreatorUserBlockedNotification(userId), creatorId());
          break;
        // case: другие особые коды
      }

    } catch (Exception e) {
      log.error(format("Не удалось выполнить обработку Telegram исключения для польователя %s.", userId), e);
    }
  }

  @EventListener(ApplicationStartedEvent.class)
  public void restoreSubscriptions() {
    List<BotUser> botUsers = userDao.fetchAllUsers();
    if (botUsers.isEmpty()) {
      log.debug("Нет записей в БД для восстановления подписок. Пропускаю шаг.");
      return;
    }
    log.debug("Восстанавливаю подписки по {} пользователям...", botUsers.size());
    for (BotUser botUser: botUsers) {
      flowConfig.startUserFlow(botUser.getUserId(), botUser.getLevelAsType(), this::sendEventViaBot);
      log.info("Подписка пользователя {} ({}) с уровнем {} восстановлена.", botUser.getUserId(),
          botUser.getUserName(), botUser.getLevel());
    }
  }

  @Override
  public String getBotUsername() {
    return botProperties.getName();
  }

  @Override
  public String getBotToken() {
    return botProperties.getToken();
  }

  @Override
  public int creatorId() {
    return botProperties.getCreatorId();
  }
}
