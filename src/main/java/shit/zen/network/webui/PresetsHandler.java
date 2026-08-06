package shit.zen.network.webui;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import shit.zen.manager.PresetManager;

public class PresetsHandler extends AbstractHttpHandler {

    @Override
    public int handleRequest(InputStream in, OutputStream out, HttpExchange exchange) throws Throwable {
        List<Map<String, Object>> games = new ArrayList<>();
        for (PresetManager.GamePreset game : PresetManager.getPresets()) {
            Map<String, Object> gameEntry = new LinkedHashMap<>();
            gameEntry.put("id", game.id());
            gameEntry.put("name", game.name());
            gameEntry.put("description", game.description());

            List<Map<String, String>> levels = new ArrayList<>();
            for (PresetManager.PresetLevel level : game.levels().values()) {
                Map<String, String> levelEntry = new LinkedHashMap<>();
                levelEntry.put("id", level.id());
                levelEntry.put("name", level.name());
                levelEntry.put("description", level.description());
                levels.add(levelEntry);
            }
            gameEntry.put("levels", levels);
            games.add(gameEntry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("games", games);
        result.put("currentGame", PresetManager.getCurrentGame());
        result.put("currentLevel", PresetManager.getCurrentLevel());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("result", result);
        out.write(new Gson().toJson(response).getBytes(StandardCharsets.UTF_8));
        return 200;
    }
}
