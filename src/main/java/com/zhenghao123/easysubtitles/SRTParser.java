package com.zhenghao123.easysubtitles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SRTParser {
    public static class Subtitle {
        private final long startMs;
        private final long endMs;
        private final String text;

        public Subtitle(long startMs, long endMs, String text) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.text = text;
        }

        // Getters
        public long getStartMs() { return startMs; }
        public long getEndMs() { return endMs; }
        public String getText() { return text; }
    }

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3}) --> (\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    );

    public static List<Subtitle> parse(File file) throws IOException {
        List<Subtitle> subtitles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) // 强制UTF-8编码
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 尝试解析序号行（跳过无效行）
                try {
                    Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    continue;
                }

                // 读取时间行
                line = reader.readLine();
                if (line == null) break;

                Matcher matcher = TIME_PATTERN.matcher(line);
                if (!matcher.find()) continue;

                long startMs = parseTime(matcher, 1);
                long endMs = parseTime(matcher, 5);

                // 读取文本
                StringBuilder text = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                    if (text.length() > 0) text.append(" ");
                    text.append(line.trim());
                }

                subtitles.add(new Subtitle(startMs, endMs, text.toString()));
            }
        }
        return subtitles;
    }

    private static long parseTime(Matcher matcher, int offset) {
        int hours = Integer.parseInt(matcher.group(offset));
        int minutes = Integer.parseInt(matcher.group(offset + 1));
        int seconds = Integer.parseInt(matcher.group(offset + 2));
        int millis = Integer.parseInt(matcher.group(offset + 3));
        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis;
    }
}