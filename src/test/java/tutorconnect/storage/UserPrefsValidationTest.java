package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.core.GuiSettings;
import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.model.UserPrefs;

public class UserPrefsValidationTest {
    @TempDir
    private Path folder;

    @Test
    public void read_nullGuiSettings_reportsCheckedLoadFailure() throws Exception {
        assertRejected("{\"guiSettings\":null}");
    }

    @Test
    public void read_invalidDimensions_reportsCheckedLoadFailure() throws Exception {
        for (String dimension : new String[]{"0", "-1", "null", "\"NaN\"", "\"Infinity\"", "1e400"}) {
            assertRejected("{\"guiSettings\":{\"windowWidth\":" + dimension + "}}");
            assertRejected("{\"guiSettings\":{\"windowHeight\":" + dimension + "}}");
        }
    }

    @Test
    public void read_missingSettingsOrDimensions_usesDefaults() throws Exception {
        for (String json : new String[]{"{}", "{\"guiSettings\":{}}"}) {
            Path file = folder.resolve("default.json");
            Files.writeString(file, json);
            assertEquals(new UserPrefs(), new JsonUserPrefsStorage(file).readUserPrefs().orElseThrow());
        }
    }

    @Test
    public void read_validSmallWindowAndNegativeCoordinates_roundTrips() throws Exception {
        UserPrefs prefs = new UserPrefs();
        prefs.setGuiSettings(new GuiSettings(1, 2, -100, -200));
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(folder.resolve("prefs.json"));
        storage.saveUserPrefs(prefs);
        assertEquals(prefs, storage.readUserPrefs().orElseThrow());
    }

    private void assertRejected(String json) throws Exception {
        Path file = folder.resolve("invalid.json");
        Files.writeString(file, json);
        assertThrows(DataLoadingException.class, () -> new JsonUserPrefsStorage(file).readUserPrefs());
        assertEquals(json, Files.readString(file));
    }
}
