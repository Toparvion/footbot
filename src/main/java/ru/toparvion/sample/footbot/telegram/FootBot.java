package ru.toparvion.sample.footbot.telegram;

import com.vdurmont.emoji.EmojiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
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
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;
import ru.toparvion.sample.footbot.dao.UserDao;
import ru.toparvion.sample.footbot.flow.BroadcastFlowConfig;
import ru.toparvion.sample.footbot.model.db.BotUser;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.springframework.util.StringUtils.hasText;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.CURRENT_MATCH_SCORE_HEADER;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.USER_ID_HEADER;
import static ru.toparvion.sample.footbot.util.Util.convertEmojies;
import static ru.toparvion.sample.footbot.util.Util.nvls;

@Slf4j
public class FootBot extends AbilityBot {

  private final String botUserName;
  private final String botToken;
  private final int botCreatorId;

  private BotSession botSession = null;
  private BroadcastFlowConfig flowConfig;
  private UserDao userDao;

  FootBot(String botUserName, String botToken, int botCreatorId, DefaultBotOptions options) {
    super(botToken, botUserName, options);
    this.botUserName = botUserName;
    this.botCreatorId = botCreatorId;
    this.botToken = botToken;
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

  @Autowired
  public void setFlowConfig(BroadcastFlowConfig flowConfig) {
    this.flowConfig = flowConfig;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @SuppressWarnings("unused")
  public Ability listLevelsAbility() {
    return Ability
            .builder()
            .name("start")
            .info("Подписаться на события мундиаля")
            .locality(USER)
            .privacy(PUBLIC)
            .action(this::listLevels)
            .build();
  }

  @SuppressWarnings("unused")public Reply reactOnSelectReply() {
    return Reply.of(this::registerUser, Flag.CALLBACK_QUERY);
  }

  private void registerUser(Update update) {
    CallbackQuery callbackQuery = update.getCallbackQuery();
    Type chosenLevel = Type.valueOf(callbackQuery.getData());
    Long chatId = AbilityUtils.getChatId(update);
    User user = AbilityUtils.getUser(update);
    BotUser botUser = new BotUser(user.getId(), composeUserName(user), chosenLevel.name());
    log.info("Сформирован пользовательский выбор: {}", botUser);
    userDao.saveUser(botUser);
    flowConfig.startUserFlow(user.getId(), chosenLevel, this::sendEventViaBot);

    Message originMessage = callbackQuery.getMessage();
    String emoji = EmojiManager.getForAlias("white_check_mark").getUnicode();
    if (originMessage != null) {
      EditMessageText editMessage = new EditMessageText();
      editMessage.setMessageId(originMessage.getMessageId());
      editMessage.enableMarkdown(true);
      editMessage.setChatId(chatId);
      editMessage.setText(emoji + " Активирована подписка на " + chosenLevel);
      silent.execute(editMessage);
    } else {
      silent.sendMd(emoji + " Активирована подписка на " + chosenLevel, chatId);
    }
  }

  private Integer sendEventViaBot(Event event, Map<String, Object> metaData) {
    String text2Send = composeEventText(event, metaData);
    try {
      Integer userId = (Integer) metaData.get(USER_ID_HEADER);
      SendMessage message = new SendMessage(String.valueOf(userId), text2Send);
      message.enableMarkdown(true);
      Message sentMessage = sender.execute(message);
      Integer messageId = sentMessage.getMessageId();
      log.debug("Отправлено Telegram сообщение с id={}: {}", messageId, sentMessage.getText());
      return messageId;

    } catch (TelegramApiException e) {
      log.error("Не удалось отправить сообщение.", e);
      return null;
    }
  }

  private void listLevels(MessageContext ctx) {
    String text = "Выбери желаемый уровень подробностей:";
    SendMessage sendMessageCommand = new SendMessage(ctx.chatId(), text);
    sendMessageCommand.enableMarkdown(true);

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
    for (Type type: Type.values()) {
      InlineKeyboardButton button = new InlineKeyboardButton(type.name().toUpperCase());
      button.setCallbackData(type.name());
      buttons.add(singletonList(button));
    }
    inlineKeyboardMarkup.setKeyboard(buttons);
    sendMessageCommand.setReplyMarkup(inlineKeyboardMarkup);
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

  private String composeEventText(Event event, Map<String, Object> metaData) {
    StringBuilder sb = new StringBuilder();
    if (!"0’".equals(event.getFullMinute())) {
      sb.append("_").append(event.getFullMinute()).append("_  ");
    }
    switch (event.getType()) {
      case text:
        sb.append(convertEmojies(event.getText()));
        break;
      case change:
        sb.append(String.format("Замена (%s): %s -> %s", event.getCommand().getName(),
            event.getPlayerOut().getName(), event.getPlayerIn().getName()));
        break;
      case statechange:
        sb.append(event.getChangedStateName());
        if ("1".equals(event.getChangedStateId())) {      // 1 - end of match
          String score = (String) metaData.get(CURRENT_MATCH_SCORE_HEADER);
          if (hasText(score)) {
            sb.append(". Счёт ").append(score);
          }
        }
        break;
      case goal:
        sb.append(String.format("%s\n(автор гола: %s (%s), текущий счёт: %s)", convertEmojies(event.getText()),
            event.getPlayer().getName(), event.getCommand().getName(), event.getInfo().getScore()));
        break;
      case card:
        switch (event.getKind()) {
          case yellow:
            sb.append("Жёлтая");
            break;
          case yellow2:
            sb.append("Вторая жёлтая");
            break;
          case red:
            sb.append("Красная");
            break;
        }
        sb.append(" карточка: ")
            .append(event.getPlayer().getName())
            .append(" (")
            .append(event.getCommand().getName())
            .append(")");
        break;
      default:
        sb.append(convertEmojies(nvls(event.getText(), "")));
    }
    return sb.toString();
  }

  private String composeUserName(User user) {
    StringBuilder sb = new StringBuilder(user.getFirstName());
    if (hasText(user.getLastName())) {
      sb.append(' ').append(user.getLastName());
    }
    if (hasText(user.getUserName())) {
      sb.append(" (@").append(user.getUserName()).append(")");
    }
    return sb.toString();
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
