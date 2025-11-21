package com.arcadeblocks.utils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Утилиты для настройки консоли
 */
public class ConsoleUtils {
    
    /**
     * Настраивает консоль для корректного отображения UTF-8 на Windows
     */
    public static void setupConsoleEncoding() {
        try {
            // Устанавливаем UTF-8 для вывода
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
            
            // Настраиваем java.util.logging для UTF-8
            setupLoggingEncoding();
            
            // Для Windows - пытаемся установить кодовую страницу через chcp
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                try {
                    // Выполняем команду chcp 65001 (UTF-8)
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "chcp", "65001");
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    Process process = pb.start();
                    process.waitFor();
                } catch (Exception e) {
                    // Игнорируем ошибки - не критично
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to setup UTF-8 encoding: " + e.getMessage());
        }
    }
    
    /**
     * Настраивает java.util.logging для вывода в UTF-8
     */
    private static void setupLoggingEncoding() {
        try {
            // Получаем корневой логгер
            Logger rootLogger = Logger.getLogger("");
            
            // Удаляем все существующие обработчики
            java.util.logging.Handler[] handlers = rootLogger.getHandlers();
            for (java.util.logging.Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }
            
            // Создаем новый ConsoleHandler с UTF-8
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setEncoding("UTF-8");
            
            // Устанавливаем простой форматтер
            consoleHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%6$s%n",
                        new java.util.Date(record.getMillis()),
                        record.getSourceClassName(),
                        record.getSourceMethodName(),
                        record.getLevel().getLocalizedName(),
                        record.getMessage(),
                        ""
                    );
                }
            });
            
            // Добавляем обработчик к корневому логгеру
            rootLogger.addHandler(consoleHandler);
            
        } catch (Exception e) {
            System.err.println("Failed to setup logging encoding: " + e.getMessage());
        }
    }
    
    /**
     * Выводит сообщение в консоль с корректной кодировкой
     */
    public static void println(String message) {
        try {
            System.out.println(message);
        } catch (Exception e) {
            // Fallback to ASCII if UTF-8 fails
            System.out.println(toAscii(message));
        }
    }
    
    /**
     * Выводит ошибку в консоль с корректной кодировкой
     */
    public static void printError(String message) {
        try {
            System.err.println(message);
        } catch (Exception e) {
            // Fallback to ASCII if UTF-8 fails
            System.err.println(toAscii(message));
        }
    }
    
    /**
     * Конвертирует строку в ASCII (удаляет не-ASCII символы)
     */
    private static String toAscii(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[^\\x00-\\x7F]", "?");
    }
}

