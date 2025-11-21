package com.arcadeblocks.localization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Labeled;
import javafx.scene.text.Text;

/**
 * Centralized localization manager responsible for translating UI strings and
 * reapplying them whenever the active game language changes.
 */
public final class LocalizationManager {

    private static final LocalizationManager INSTANCE = new LocalizationManager();
    private static final ResourceBundle.Control UTF8_CONTROL = new UTF8Control();
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("en");

    private final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(DEFAULT_LOCALE);

    private LocalizationManager() {
        // Singleton
    }

    public static LocalizationManager getInstance() {
        return INSTANCE;
    }

    public ObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    public Locale getLocale() {
        return localeProperty.get();
    }

    public void setLanguage(String languageCode) {
        Locale newLocale = safeLocale(languageCode);
        if (!newLocale.equals(localeProperty.get())) {
            localeProperty.set(newLocale);
        }
    }

    public String get(String key) {
        return get(key, localeProperty.get());
    }

    public String format(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        if (value == null || value.equals(key)) {
            return defaultValue;
        }
        return value;
    }

    public String formatOrDefault(String key, String defaultPattern, Object... args) {
        String pattern = getOrDefault(key, defaultPattern);
        return MessageFormat.format(pattern, args);
    }

    public StringBinding bind(Labeled labeled, String key) {
        return bind(labeled, key, () -> EMPTY_ARGS);
    }

    public StringBinding bind(Labeled labeled, String key, Supplier<Object[]> argsSupplier) {
        StringBinding binding = Bindings.createStringBinding(
            () -> format(key, argsSupplier != null ? argsSupplier.get() : EMPTY_ARGS),
            localeProperty
        );
        labeled.textProperty().bind(binding);
        return binding;
    }

    public StringBinding bind(Text textNode, String key) {
        return bind(textNode, key, () -> EMPTY_ARGS);
    }

    public StringBinding bind(Text textNode, String key, Supplier<Object[]> argsSupplier) {
        StringBinding binding = Bindings.createStringBinding(
            () -> format(key, argsSupplier != null ? argsSupplier.get() : EMPTY_ARGS),
            localeProperty
        );
        textNode.textProperty().bind(binding);
        return binding;
    }

    private String get(String key, Locale locale) {
        ResourceBundle bundle = getBundle(locale);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        if (!locale.equals(DEFAULT_LOCALE)) {
            // fallback to default locale
            return get(key, DEFAULT_LOCALE);
        }
        return key; // fallback to key if translation missing in default bundle
    }

    private ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle("i18n.messages", locale, UTF8_CONTROL);
        } catch (MissingResourceException | NullPointerException e) {
            if (!locale.equals(DEFAULT_LOCALE)) {
                return getBundle(DEFAULT_LOCALE);
            }
            // If bundle is not loaded yet, return null instead of throwing
            return null;
        }
    }

    private Locale safeLocale(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LOCALE;
        }
        Locale locale = Locale.forLanguageTag(languageCode);
        if (locale.getLanguage() == null || locale.getLanguage().isBlank()) {
            return DEFAULT_LOCALE;
        }
        return locale;
    }

    private static final Object[] EMPTY_ARGS = new Object[0];

    private static final class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, java.io.IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (java.io.InputStream stream = reload
                    ? loader.getResource(resourceName) == null ? null
                        : loader.getResource(resourceName).openConnection().getInputStream()
                    : loader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    return null;
                }
                try (java.io.Reader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    return new java.util.PropertyResourceBundle(reader);
                }
            }
        }
    }
}
