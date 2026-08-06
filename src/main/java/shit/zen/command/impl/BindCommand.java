package shit.zen.command.impl;

import com.mojang.blaze3d.platform.InputConstants;
import shit.zen.ZenClient;
import shit.zen.command.Command;
import shit.zen.event.EventTarget;
import shit.zen.exception.ModuleNotFoundException;
import shit.zen.modules.Module;
import shit.zen.utils.misc.ChatUtil;

public class BindCommand
extends Command {
    public static final class EventHandler {
        private final BindCommand parent;
        private final Module module;
        private final String name;

        public EventHandler(BindCommand parent, Module module, String name) {
            this.parent = parent;
            this.module = module;
            this.name = name;
        }

        @EventTarget
        public void onKey(shit.zen.event.impl.KeyEvent event) {
            if (!event.isPressed()) return;
            int keyCode = event.getKeyCode();
            if (keyCode == 256) {
                module.setKey(0);
                ChatUtil.print("已清除 " + this.module.getDisplayName() + " 的按键绑定。");
            } else {
                module.setKey(keyCode);
                ChatUtil.print("已将 " + this.module.getDisplayName() + " 绑定到按键 " + keyCode + "。");
            }
            ZenClient.getInstance().getEventBus().unregister(this);
            ZenClient.getInstance().getConfigManager().saveAll();
        }
    }

    public BindCommand() {
        super("bind", new String[]{"b"});
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            this.handleBindInteractive(args[0]);
        } else if (args.length == 2) {
            this.handleBindExplicit(args[0], args[1]);
        } else {
            ChatUtil.print("用法：.bind <模块> [按键]");
        }
    }

    private void handleBindInteractive(String moduleName) {
        try {
            Module module = ZenClient.getInstance().getModuleManager().getModule(moduleName);
            if (module == null) {
                ChatUtil.print("模块无效。");
                return;
            }
            ChatUtil.print("请按下要绑定到 " + module.getDisplayName() + " 的按键。");
            ZenClient.getInstance().getEventBus().register(new BindCommand.EventHandler(this, module, moduleName));
        } catch (ModuleNotFoundException e) {
            ChatUtil.print("模块无效。");
        }
    }

    private void handleBindExplicit(String moduleName, String keyName) {
        try {
            Module module = ZenClient.getInstance().getModuleManager().getModule(moduleName);
            if (module == null) {
                ChatUtil.print("模块无效。");
                return;
            }
            if (keyName.equalsIgnoreCase("none")) {
                module.setKey(InputConstants.UNKNOWN.getValue());
                ChatUtil.print("已清除 " + module.getDisplayName() + " 的按键绑定。");
                ZenClient.getInstance().getConfigManager().saveAll();
                return;
            }
            InputConstants.Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase());
            if (key == InputConstants.UNKNOWN) {
                ChatUtil.print("按键无效。");
                return;
            }
            module.setKey(key.getValue());
            ChatUtil.print("已将 " + module.getDisplayName() + " 绑定到 " + keyName.toUpperCase() + "。");
            ZenClient.getInstance().getConfigManager().saveAll();
        } catch (ModuleNotFoundException e) {
            ChatUtil.print("模块无效。");
        }
    }

    @Override
    public String[] onTab(String[] stringArray) {
        return ZenClient.getInstance().getModuleManager().getModules().stream().map(Module::getName).filter(string -> string.toLowerCase().startsWith(stringArray.length == 0 ? "" : stringArray[0].toLowerCase())).toArray(String[]::new);
    }
}
