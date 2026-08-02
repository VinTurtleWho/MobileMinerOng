package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarParser {
    private static final Pattern MANA_PATTERN = Pattern.compile("(\\d+)/(\\d+)✎\\s*Mana");

    public static void parseActionBarText(String text, BotContext ctx) {
        if (text == null || text.isEmpty()) return;

        Matcher manaMatcher = MANA_PATTERN.matcher(text);
        if (manaMatcher.find()) {
            try {
                int current = Integer.parseInt(manaMatcher.group(1));
                int max = Integer.parseInt(manaMatcher.group(2));
                ctx.setMana(current, max);
            } catch (NumberFormatException ignored) {}
        }
    }
}
