package ru.toparvion.sample.footbot.telegram;

import lombok.extern.slf4j.Slf4j;
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

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.USER_ID_HEADER;

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
    if (botSession != null) {
      log.warn("Bot session is already started. Skipping double start.");
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
  }

  private Integer sendEventViaBot(Event event, Map<String, Object> metaData) {
    try {
      Integer userId = (Integer) metaData.get(USER_ID_HEADER);
      SendMessage message = new SendMessage(valueOf(userId), helper.composeEventText(event, metaData));
      message.enableMarkdown(true);
      Message sentMessage = sender.execute(message);
      Integer messageId = sentMessage.getMessageId();
      log.debug("Отправлено Telegram сообщение с id={}: {}", messageId, sentMessage.getText());
      return messageId;

    } catch (TelegramApiRequestException e) {
      log.error(format("Не удалось отправить сообщение: %s", e.toString()), e);
      return null;

    } catch (TelegramApiException e) {
      log.error("Не удалось отправить сообщение: ", e);
      return null;
    }
  }

  private void listLevels(MessageContext ctx) {
    SendMessage sendMessageCommand = new SendMessage();
    sendMessageCommand.setChatId(ctx.chatId());
    helper.composeSelectMarkup(sendMessageCommand);
    silent.execute(sendMessageCommand);
    log.info("Отправлены варианты выбора.");
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
      log.debug("Подписка пользователя {} ({}) с уровнем {} восстановлена.", botUser.getUserId(),
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
