package ru.toparvion.sample.footbot.telegram;

import com.vdurmont.emoji.EmojiParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.toparvion.sample.footbot.model.config.Schedule;
import ru.toparvion.sample.footbot.model.config.ScheduledMatch;
import ru.toparvion.sample.footbot.model.db.BotUser;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.springframework.util.StringUtils.hasText;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.CURRENT_MATCH_OVERTIME_SCORE_HEADER;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.CURRENT_MATCH_SCORE_HEADER;
import static ru.toparvion.sample.footbot.util.Util.convertEmojies;
import static ru.toparvion.sample.footbot.util.Util.nvls;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InteractionHelper implements ApplicationContextAware {

  private final Schedule schedule;
  private ApplicationContext appContext;
  private final Locale locale = new Locale("ru", "RU");

  void composeSelectMarkup(SendMessage sendMessageCommand, @Nullable Type chosenType) {
    sendMessageCommand.enableMarkdown(true);
    sendMessageCommand.setText(getMessage("start.select-description"));

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
    Type[] types = Type.values();
    int startIdx = types.length - 4;
    for (int i = startIdx; i >= 0; i--) {
      Type type = types[i];
      InlineKeyboardButton button = new InlineKeyboardButton();
      StringBuilder buttonText = new StringBuilder(getMessage("level." + type.name()));
      if ((i <= (types.length - 6)) && (i >= 1)) {
        buttonText.insert(0, "и ");
      }
      if (type == chosenType) {
        buttonText.append(" ").append(getMessage("level.chosen.suffix"));
      }
      button.setText(EmojiParser.parseToUnicode(buttonText.toString()));
      button.setCallbackData(type.name());
      buttons.add(singletonList(button));
    }
    inlineKeyboardMarkup.setKeyboard(buttons);
    sendMessageCommand.setReplyMarkup(inlineKeyboardMarkup);
  }

  String composeLevelSelectAnswer(Type selectedLevel) {
    String answerText;
    String nearestMatchTime = findNearestMatchTime();
    switch (selectedLevel) {
      case text:
        answerText = String.format("%s\n%s\n%s\n\n%s",
            getMessage("subscription.activated.prefix"),
            getMessage("level." + selectedLevel.name()),
            getMessage("subscription.activated.suffix"),
            getMessage("subscription.activated.postfix") + nearestMatchTime);
        break;
      case none:
        answerText = getMessage("subscription.deactivated-answer");
        break;
      default:
        StringBuilder levels = new StringBuilder();
        Type[] types = Type.values();
        int startIdx = types.length - 5;    // to account two penalty types and artificial type 'none'
        for (int i = startIdx; i >= selectedLevel.ordinal(); i--) {
          String level = types[i].name();
          String levelText = getMessage("level." + level);
          if (i != startIdx) {
            levels.append('\n')
                  .append("и ");
          }
          levels.append(levelText);
        }
        answerText = String.format("%s\n%s\n%s\n\n%s",
            getMessage("subscription.activated.prefix"),
            levels.toString(),
            getMessage("subscription.activated.suffix"),
            getMessage("subscription.activated.postfix") + nearestMatchTime);
    }
    return EmojiParser.parseToUnicode(answerText);
  }

  private static String composeMatchTime(ScheduledMatch schedule) {
    String matchStartTime = schedule.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yy 'в' HH:mm"));
    return String.format(" _%s_ состоится *%s* по Москве.", schedule.getTitle(), matchStartTime);
  }

  private String findNearestMatchTime() {
    return schedule.getSchedule().stream()
        .filter(match -> match.getDate().isAfter(now()))
        .min(comparing(ScheduledMatch::getDate))
        .map(InteractionHelper::composeMatchTime)
        .orElse(" пока не предвидится.");
  }

  String composeEventText(Event event, Map<String, Object> metaData) {
    StringBuilder sb = new StringBuilder();
    if (!"0’".equals(event.getFullMinute())) {
      sb.append("_").append(event.getFullMinute()).append("_  ");
    }
    switch (event.getType()) {
      case text:
        if (event.isDangerous()) {
          sb.append(":heavy_exclamation_mark: ");
        }
        sb.append(event.getText());
        break;
      case change:
        sb.append(":repeat: ")
          .append(String.format("Замена (%s):\n:arrow_down_small: %s\n:arrow_up_small: %s",
              event.getCommand().getName(), event.getPlayerOut().getName(), event.getPlayerIn().getName()));
        break;
      case statechange:
        sb.append(":stopwatch: ")
          .append(event.getChangedStateName());
        if ("1".equals(event.getChangedStateId())) {      // 1 - end of match
          String score = (String) metaData.get(CURRENT_MATCH_SCORE_HEADER);
          if (hasText(score)) {
            sb.append(". Счёт ").append(score);
          }
          String overtimeScore = (String) metaData.get(CURRENT_MATCH_OVERTIME_SCORE_HEADER);
          if (hasText(overtimeScore)) {
            sb.append(' ').append(overtimeScore);
          }
        }
        break;
      case goal:
      case penaltyseriegoal:
        sb.append(":soccer: ")
          .append(String.format("%s\n(автор гола: %s (%s), текущий счёт: %s)", event.getText(),
            event.getPlayer().getName(), event.getCommand().getName(), event.getInfo().getScore()));
        break;
      case penaltyserienogoal:
      case penaltynogoal:
        sb.append(":no_entry_sign: ")
          .append(event.getText());
        break;
      case card:
        sb.append(":warning: ");
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
        sb.append(nvls(event.getText(), ""));
    }
    return convertEmojies(sb.toString());
  }

  String composeHelpText() {
    String helpText = getMessage("help.text");
    return EmojiParser.parseToUnicode(helpText);
  }

  String composeUserName(User user) {
    StringBuilder sb = new StringBuilder(user.getFirstName());
    if (hasText(user.getLastName())) {
      sb.append(' ').append(user.getLastName());
    }
    if (hasText(user.getUserName())) {
      sb.append(" (@").append(user.getUserName()).append(")");
    }
    return sb.toString();
  }

  String composeCreatorUserRegistrationNotification(BotUser botUser) {
    String message = getMessage("creator.notification.user-registration", botUser.getUserName(),
        botUser.getUserId(), botUser.getLevel());
    return EmojiParser.parseToUnicode(message);
  }

  String composeCreatorUserBlockedNotification(int userId) {
    String message = getMessage("creator.notification.blocked-user-excluded", userId);
    return EmojiParser.parseToUnicode(message);
  }

  private String getMessage(String code) {
    return getMessage(code, (Object[]) null);
  }

  private String getMessage(String code, Object ... args) {
    return appContext.getMessage(code, args, locale);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    appContext = applicationContext;
  }
}
