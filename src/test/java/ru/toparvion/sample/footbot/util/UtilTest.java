package ru.toparvion.sample.footbot.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {
  @Test
  public void testConverter() {
    String converted = Util.convertEmojies("Просто ::cry::какой-то: пример ::smiley::");
    System.out.println(converted);
    assertEquals("Просто \uD83D\uDE22какой-то: пример \uD83D\uDE03", converted);
  }
}