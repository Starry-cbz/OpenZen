package shit.zen.network.webui;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import shit.zen.manager.PresetManager;
import shit.zen.utils.render.TextureUtil;

public class ApplyPresetHandler extends AbstractHttpHandler {

    @Override
    public int handleRequest(InputStream in, OutputStream out, HttpExchange exchange) throws Throwable {
        Map<String, String> query = TextureUtil.parseQueryString(exchange.getRequestURI().getQuery());
        String game = query.get("game");
        String level = query.get("level");
        Map<String, Object> response = new LinkedHashMap<>();

        if (game == null || level == null) {
            response.put("success", false);
            response.put("reason", "参数不足");
        } else if (PresetManager.getPreset(game, level) == null) {
            response.put("success", false);
            response.put("reason", "未知的预设");
        } else {
            try {
                PresetManager.apply(game, level);
                Map<String, String> result = new LinkedHashMap<>();
                result.put("game", game);
                result.put("level", level);
                response.put("success", true);
                response.put("result", result);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                response.put("success", false);
                response.put("reason", throwable.toString());
            }
        }

        out.write(new Gson().toJson(response).getBytes(StandardCharsets.UTF_8));
        return 200;
    }
}
