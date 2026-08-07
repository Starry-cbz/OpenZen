package shit.zen.command.impl;

import shit.zen.ZenClient;
import shit.zen.command.Command;
import shit.zen.utils.misc.ChatUtil;

/** In-game command for stopping the injected client without closing Minecraft. */
public final class UnloadCommand extends Command {
    public UnloadCommand() {
        super("unload", new String[]{"exit", "disable", "detach"});
    }

    @Override
    public void onCommand(String[] args) {
        ZenClient client = ZenClient.getInstance();
        if (client != null) {
            client.unloadInGame();
            ChatUtil.print("注入程序已退出，游戏仍在运行。");
        }
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}
