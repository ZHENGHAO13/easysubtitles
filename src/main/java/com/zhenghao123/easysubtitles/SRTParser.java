package com.zhenghao123.easysubtitles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class SRTParser {
    public static class Subtitle {
        private final long start;
        private final long end;
        private final String text;

        public Subtitle(long start, long end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }

        public long getStartMs() { return start; }
        public long getEndMs() { return end; }
        public String getText() { return text; }
    }

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3}) --> (\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    );

    public static List<Subtitle> parse(File file) throws IOException {
        List<Subtitle> subs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 跳过序号行
                if (!line.matches("\\d+")) continue;

                // 解析时间轴
                line = reader.readLine();
                Matcher matcher = TIME_PATTERN.matcher(line);
                if (!matcher.find()) continue;

                long start = parseTime(matcher, 1);
                long end = parseTime(matcher, 5);

                // 读取文本
                StringBuilder text = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    text.append(line).append(" ");
                }

                subs.add(new Subtitle(start, end, text.toString().trim()));
            }
        }
        return subs;
    }

    private static long parseTime(Matcher m, int offset) {
        int h = Integer.parseInt(m.group(offset));
        int min = Integer.parseInt(m.group(offset + 1));
        int sec = Integer.parseInt(m.group(offset + 2));
        int ms = Integer.parseInt(m.group(offset + 3));
        return h * 3600000L + min * 60000L + sec * 1000L + ms;
    }
}