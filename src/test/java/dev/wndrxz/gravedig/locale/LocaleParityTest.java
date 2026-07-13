package dev.wndrxz.gravedig.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * en.yml and ru.yml must stay in lockstep. tiny hand-rolled yaml walker so
 * the test needs neither bukkit nor snakeyaml on the classpath.
 */
class LocaleParityTest {

    @Test
    void ruHasEveryEnKeyAndViceVersa() {
        Map<String, String> en = flatten("en");
        Map<String, String> ru = flatten("ru");
        Set<String> onlyEn = new TreeSet<>(en.keySet());
        onlyEn.removeAll(ru.keySet());
        Set<String> onlyRu = new TreeSet<>(ru.keySet());
        onlyRu.removeAll(en.keySet());
        assertTrue(onlyEn.isEmpty(), "missing in ru.yml: " + onlyEn);
        assertTrue(onlyRu.isEmpty(), "missing in en.yml: " + onlyRu);
    }

    @Test
    void noBlankValues() {
        for (String lang : List.of("en", "ru")) {
            flatten(lang).forEach((key, value) ->
                    assertFalse(value.isBlank(), lang + ".yml has blank value for " + key));
        }
    }

    // keys the code references by string literal. rename here = rename in code
    @Test
    void coreKeysArePinned() {
        Map<String, String> en = flatten("en");
        for (String key : List.of("prefix", "death.grave-created", "dig.protected",
                "dig.portion-armor", "dig.portion-hotbar", "dig.portion-rest",
                "dig.xp", "dig.done", "grave.expired", "chat.click-to-copy",
                "command.reloaded")) {
            assertTrue(en.containsKey(key), "en.yml lost pinned key: " + key);
        }
    }

    /** flat "a.b.c" -> value map. assumes 2-space indent, which our files use */
    private Map<String, String> flatten(String lang) {
        Map<String, String> out = new LinkedHashMap<>();
        List<String> path = new ArrayList<>();
        for (String line : readLines(lang)) {
            if (line.isBlank() || line.strip().startsWith("#")) continue;
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') indent++;
            int depth = indent / 2;
            int colon = line.indexOf(':', indent);
            if (colon < 0) continue;
            String key = line.substring(indent, colon).strip();
            String value = line.substring(colon + 1).strip();
            while (path.size() > depth) path.remove(path.size() - 1);
            path.add(key);
            if (!value.isEmpty()) {
                out.put(String.join(".", path), unquote(value));
                path.remove(path.size() - 1);
            }
        }
        return out;
    }

    private String unquote(String v) {
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private List<String> readLines(String lang) {
        // main resources land on the test classpath thanks to the java plugin
        InputStream in = getClass().getResourceAsStream("/lang/" + lang + ".yml");
        assertNotNull(in, "lang/" + lang + ".yml not on classpath");
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lines;
    }
}
