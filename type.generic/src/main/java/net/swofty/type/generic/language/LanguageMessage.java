package net.swofty.type.generic.language;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public enum LanguageMessage {
    CURRENT_LANGUAGE_KEY("language.current"),
    AVAILABLE_LANGUAGES_KEY("language.available"),
    USE_LANGUAGE_HINT_KEY("language.hint.use_command"),
    UNKNOWN_LANGUAGE_KEY("language.error.unknown"),
    LANGUAGE_UPDATED_KEY("language.updated");

    // Legacy string constants kept for compatibility with code using the previous API shape.
    public static final String CURRENT_LANGUAGE = CURRENT_LANGUAGE_KEY.key();
    public static final String AVAILABLE_LANGUAGES = AVAILABLE_LANGUAGES_KEY.key();
    public static final String USE_LANGUAGE_HINT = USE_LANGUAGE_HINT_KEY.key();
    public static final String UNKNOWN_LANGUAGE = UNKNOWN_LANGUAGE_KEY.key();
    public static final String LANGUAGE_UPDATED = LANGUAGE_UPDATED_KEY.key();

    private static final Map<PlayerLanguage, Map<String, String>> MESSAGES = new EnumMap<>(PlayerLanguage.class);
    private static final LanguageAdventureTranslator ADVENTURE_TRANSLATOR = new LanguageAdventureTranslator();

    static {
        Yaml yaml = new Yaml();
        for (PlayerLanguage language : PlayerLanguage.values()) {
            MESSAGES.put(language, loadMessages(yaml, language));
        }
    }

    private final String key;

    LanguageMessage(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String format(PlayerLanguage language, Object... args) {
        return formatByCode(key, language, args);
    }

    public MessageFormat toMessageFormat(PlayerLanguage language) {
        return new MessageFormat(resolveTemplate(key, language), language.getLocale());
    }

    public static String formatByCode(String code, PlayerLanguage language, Object... args) {
        return String.format(resolveTemplate(code, language), args);
    }

    static String resolveTemplate(String key, Locale locale) {
        PlayerLanguage language = PlayerLanguage.fromInput(locale.toLanguageTag());
        if (language == null) {
            language = PlayerLanguage.ENGLISH;
        }
        return resolveTemplate(key, language);
    }

    static String resolveTemplate(String key, PlayerLanguage language) {
        Map<String, String> localized = MESSAGES.get(language);
        if (localized != null && localized.containsKey(key)) {
            return localized.get(key);
        }

        Map<String, String> english = MESSAGES.get(PlayerLanguage.ENGLISH);
        if (english != null && english.containsKey(key)) {
            return english.get(key);
        }

        return key;
    }

    public static LanguageMessage fromCode(String code) {
        return Arrays.stream(values())
                .filter(message -> message.key.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static LanguageAdventureTranslator adventureTranslator() {
        return ADVENTURE_TRANSLATOR;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadMessages(Yaml yaml, PlayerLanguage language) {
        String resourcePath = "lang/messages_" + language.getId() + ".yml";

        try (InputStream input = LanguageMessage.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing language resource: " + resourcePath);
            }

            Object loaded = yaml.load(input);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IllegalStateException("Invalid YAML structure for: " + resourcePath);
            }

            return (Map<String, String>) map;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load language resource: " + resourcePath, exception);
        }
    }
}
