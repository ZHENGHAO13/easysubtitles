package com.zhenghao123.easysubtitles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger();

    public static List<Subtitle> parse(File file) throws IOException {
        LOGGER.info("解析SRT文件: {}", file.getName());
        List<Subtitle> subs = new ArrayList<>();
        int count = 0;
        int errorCount = 0;

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
                if (line == null) {
                    LOGGER.warn("文件提前结束，缺少时间轴行");
                    break; // 防止文件意外结束
                }

                Matcher matcher = TIME_PATTERN.matcher(line);
                if (!matcher.find()) {
                    LOGGER.warn("无效的时间轴格式: {}", line);
                    errorCount++;
                    continue;
                }

                long start, end;
                try {
                    start = parseTime(matcher, 1);
                    end = parseTime(matcher, 5);
                } catch (NumberFormatException e) {
                    LOGGER.warn("时间解析错误: {} - {}", line, e.getMessage());
                    errorCount++;
                    continue;
                }

                // 读取文本
                StringBuilder text = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    text.append(line).append("\n");
                }

                if (text.length() > 0) {
                    text.setLength(text.length() - 1); // 移除最后一个换行符
                }

                subs.add(new Subtitle(start, end, text.toString().trim()));
                count++;
            }

            LOGGER.info("成功解析 {} 个字幕块，跳过 {} 个无效块", count, errorCount);
            return subs;
        } catch (Exception e) {
            throw new IOException("解析失败: " + file.getName() + " - " + e.getMessage());
        }
    }

    private static long parseTime(Matcher m, int offset) {
        int h = Integer.parseInt(m.group(offset));
        int min = Integer.parseInt(m.group(offset + 1));
        int sec = Integer.parseInt(m.group(offset + 2));
        int ms = Integer.parseInt(m.group(offset + 3));
        return h * 3600000L + min * 60000L + sec * 1000L + ms;
    }
}