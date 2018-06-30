package ru.toparvion.sample.footbot.telegram;

import com.vdurmont.emoji.EmojiParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.springframework.util.StringUtils.hasText;
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
public class InteractionHelper implements InitializingBean {

  private final ResourceLoader resourceLoader;
  private Properties textProps;

  void composeSelectMarkup(SendMessage sendMessageCommand) {
    sendMessageCommand.enableMarkdown(true);
    sendMessageCommand.setText(textProps.getProperty("start.select-description"));

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
    Type[] values = Type.values();
    for (int i = values.length-1; i >= 0; i--) {
      Type type = values[i];
      InlineKeyboardButton button = new InlineKeyboardButton();
      String buttonText = EmojiParser.parseToUnicode(textProps.getProperty("level." + type.name(), type.name()));
      button.setText(buttonText);
      button.setCallbackData(type.name());
      buttons.add(singletonList(button));
    }
    inlineKeyboardMarkup.setKeyboard(buttons);
    sendMessageCommand.setReplyMarkup(inlineKeyboardMarkup);
  }

  String composeLevelSelectAnswer(Type selectedLevel) {
    String answerText;
    switch (selectedLevel) {
      case text:
        answerText = String.format("%s\n- %s\n%s",
            textProps.getProperty("subscription.activated.prefix"),
            textProps.getProperty("level." + selectedLevel.name()),
            textProps.getProperty("subscription.activated.suffix"));
        break;
      case none:
        answerText = textProps.getProperty("subscription.deactivated-answer");
        break;
      default:
        StringBuilder levels = new StringBuilder();
        for (int i = selectedLevel.ordinal(); i <= Type.values().length-2; i++) {
          String level = Type.values()[i].name();
          String levelText = textProps.getProperty("level." + level);
          levels.append("- ")
                .append(levelText)
                .append('\n');
        }
        answerText = String.format("%s\n%s\n%s",
            textProps.getProperty("subscription.activated.prefix"),
            levels.toString(),
            textProps.getProperty("subscription.activated.suffix"));
    }
    return EmojiParser.parseToUnicode(answerText);
  }

  String composeEventText(Event event, Map<String, Object> metaData) {
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

  @Override
  public void afterPropertiesSet() throws Exception {
    Resource textResource = resourceLoader.getResource("classpath:text.properties");
    textProps = new Properties();
    InputStreamReader isr = new InputStreamReader(textResource.getInputStream(), StandardCharsets.UTF_8);
    textProps.load(isr);
    log.debug("Текстовый ресурс успещно загружен: {} сообщений.", textProps.size());
  }
}
