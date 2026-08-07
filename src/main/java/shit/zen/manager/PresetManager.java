package shit.zen.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import shit.zen.ZenClient;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.BooleanSetting;
import shit.zen.settings.impl.ModeSetting;
import shit.zen.settings.impl.MultiSelectSetting;
import shit.zen.settings.impl.NumberSetting;

/** Applies the WebUI's built-in gameplay presets without changing config keys. */
public final class PresetManager {
    public static final String BEDWARS_XP = "bedwars_xp";
    public static final String SKYWARS = "skywars";
    public static final String SAFE = "SAFE";
    public static final String NORMAL = "NORMAL";
    public static final String MAX = "MAX";

    private static final Map<String, GamePreset> PRESETS = createPresets();
    private static String currentGame = BEDWARS_XP;
    private static String currentLevel = SAFE;

    private PresetManager() {
    }

    public static synchronized List<GamePreset> getPresets() {
        return List.copyOf(PRESETS.values());
    }

    public static synchronized PresetLevel getPreset(String game, String level) {
        GamePreset gamePreset = PRESETS.get(game);
        if (gamePreset == null) {
            return null;
        }
        return gamePreset.levels().get(level);
    }

    public static synchronized void apply(String game, String level) {
        PresetLevel preset = getPreset(game, level);
        if (preset == null) {
            throw new IllegalArgumentException("未知的预设");
        }

        ModuleManager moduleManager = ZenClient.getInstance().getModuleManager();
        for (ModulePreset modulePreset : preset.modules()) {
            Module module = moduleManager.getModule(modulePreset.module());
            for (SettingValue setting : modulePreset.settings()) {
                validateValue(module, setting.name(), setting.value());
            }
        }

        for (Module module : moduleManager.getModules()) {
            boolean preserved = module.getCategory() == Category.RENDER || "WebUI".equals(module.getName());
            if (!preserved && module.isEnabled()) {
                module.setEnabled(false);
            }
        }

        for (ModulePreset modulePreset : preset.modules()) {
            Module module = moduleManager.getModule(modulePreset.module());
            for (SettingValue setting : modulePreset.settings()) {
                setValue(module, setting.name(), setting.value());
            }
            if (modulePreset.enabled() && !"Scaffold".equals(module.getName())) {
                module.setEnabled(true);
            }
        }
        // No gameplay preset may enable automatic bridging.
        Module scaffold = moduleManager.getModule("Scaffold");
        if (scaffold.isEnabled()) {
            scaffold.setEnabled(false);
        }

        currentGame = game;
        currentLevel = level;
        ZenClient.getInstance().getConfigManager().saveAll();
    }

    public static synchronized String getCurrentGame() {
        return currentGame;
    }

    public static synchronized String getCurrentLevel() {
        return currentLevel;
    }

    private static void setValue(Module module, String name, Object value) {
        for (Setting<?> setting : module.getSettings()) {
            if (!setting.getName().equals(name)) {
                continue;
            }
            if (setting instanceof BooleanSetting booleanSetting && value instanceof Boolean booleanValue) {
                booleanSetting.setValue(booleanValue);
            } else if (setting instanceof NumberSetting numberSetting && value instanceof Number numberValue) {
                numberSetting.setValue(numberValue);
            } else if (setting instanceof ModeSetting modeSetting && value instanceof String stringValue
                    && Arrays.asList(modeSetting.getModes()).contains(stringValue)) {
                modeSetting.setValue(stringValue);
            } else if (setting instanceof MultiSelectSetting multiSelectSetting && value instanceof List<?> values) {
                List<String> selected = values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
                if (multiSelectSetting.getOptions().containsAll(selected)) {
                    multiSelectSetting.setValue(new ArrayList<>(selected));
                }
            }
            return;
        }
        throw new IllegalArgumentException("模块 " + module.getName() + " 不存在设置 " + name);
    }

    private static void validateValue(Module module, String name, Object value) {
        for (Setting<?> setting : module.getSettings()) {
            if (!setting.getName().equals(name)) {
                continue;
            }
            boolean valid = setting instanceof BooleanSetting && value instanceof Boolean;
            if (setting instanceof NumberSetting numberSetting && value instanceof Number numberValue) {
                double number = numberValue.doubleValue();
                valid = number >= numberSetting.getMin().doubleValue() && number <= numberSetting.getMax().doubleValue();
            } else if (setting instanceof ModeSetting modeSetting && value instanceof String stringValue) {
                valid = Arrays.asList(modeSetting.getModes()).contains(stringValue);
            } else if (setting instanceof MultiSelectSetting multiSelectSetting && value instanceof List<?> values) {
                List<String> selected = values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
                valid = selected.size() == values.size() && multiSelectSetting.getOptions().containsAll(selected);
            }
            if (!valid) {
                throw new IllegalArgumentException("模块 " + module.getName() + " 的设置 " + name + " 值无效");
            }
            return;
        }
        throw new IllegalArgumentException("模块 " + module.getName() + " 不存在设置 " + name);
    }

    private static Map<String, GamePreset> createPresets() {
        Map<String, GamePreset> presets = new LinkedHashMap<>();
        presets.put(BEDWARS_XP, new GamePreset(
                BEDWARS_XP, "起床战争（经验模式）", "经验模式的预设组合。任何档位仍可能触发服务器处罚。",
                levels(
                        level(SAFE, "安全", "低风险档：以辅助和整理为主，仍可能被服务器处罚。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("AutoTools", true),
                                module("InventoryManager", true, setting("Inventory Only", true)),
                                module("AutoPlay", true, setting("Delay", 4.0))),
                        level(NORMAL, "正常", "中风险档：加入适度战斗辅助，存在封禁可能；自动搭路保持关闭。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("AutoTools", true),
                                module("InventoryManager", true, setting("Inventory Only", true)),
                                module("AutoPlay", true, setting("Delay", 3.0)),
                                module("AimAssist", true, setting("Range", 4.0), setting("Smooth amount", 25.0)),
                                module("AutoClicker", true, setting("CPS", 8.0), setting("Mode", "Left"))),
                        level(MAX, "最大", "高风险档：自动化程度最高，极易被服务器封禁。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("AutoTools", true),
                                module("InventoryManager", true, setting("Inventory Only", true), setting("Fast Throw", true)),
                                module("AutoPlay", true, setting("Delay", 1.0)),
                                module("AimAssist", true, setting("Range", 6.0), setting("Smooth amount", 5.0)),
                                module("AutoClicker", true, setting("CPS", 15.0), setting("Mode", "Left"), setting("CPS Mode", "DBC")),
                                module("KillAura", true, setting("Aim Range", 6.0), setting("Max APS", 20.0), setting("Min APS", 18.0), setting("Multi Attack", true)),
                                module("AntiKB", true, setting("Mode", "NoXZ"), setting("Attack amount", 10.0), setting("Instant Attack", true)),
                                module("Critical", true),
                                module("NoFall", true),
                                module("NoSlow", true, setting("Mode", "NoSlow")))))
        );
        presets.put(SKYWARS, new GamePreset(
                SKYWARS, "空岛战争", "空岛资源与战斗的预设组合。任何档位仍可能触发服务器处罚。",
                levels(
                        level(SAFE, "安全", "低风险档：以资源整理和队伍识别为主，仍可能被服务器处罚。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("ChestStealer", true, setting("Delay", 250.0), setting("Open Delay", 3.0), setting("Random Click", true)),
                                module("InventoryManager", true, setting("Inventory Only", true)),
                                module("AutoTools", true)),
                        level(NORMAL, "正常", "中风险档：加入适度战斗辅助，存在封禁可能。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("ChestStealer", true, setting("Delay", 120.0), setting("Open Delay", 2.0), setting("Random Click", true)),
                                module("InventoryManager", true, setting("Inventory Only", true)),
                                module("AutoTools", true),
                                module("AimAssist", true, setting("Range", 4.0), setting("Smooth amount", 25.0)),
                                module("AutoClicker", true, setting("CPS", 8.0), setting("Mode", "Left")),
                                module("KillAura", true, setting("Aim Range", 4.0), setting("Max APS", 12.0), setting("Min APS", 9.0)),
                                module("NoFall", true)),
                        level(MAX, "最大", "高风险档：自动化程度最高，极易被服务器封禁。",
                                module("AntiBots", true, setting("Debug", false)),
                                module("Teams", true, setting("Mode", "Scoreboard")),
                                module("Sprint", true),
                                module("AntiStaff", true),
                                module("ChestStealer", true, setting("Delay", 0.0), setting("Open Delay", 0.0), setting("Random Click", false), setting("PickTrash", true)),
                                module("InventoryManager", true, setting("Inventory Only", true), setting("Fast Throw", true)),
                                module("AutoTools", true),
                                module("AimAssist", true, setting("Range", 6.0), setting("Smooth amount", 5.0)),
                                module("AutoClicker", true, setting("CPS", 15.0), setting("Mode", "Left"), setting("CPS Mode", "DBC")),
                                module("KillAura", true, setting("Aim Range", 6.0), setting("Max APS", 20.0), setting("Min APS", 18.0), setting("Multi Attack", true)),
                                module("AntiKB", true, setting("Mode", "NoXZ"), setting("Attack amount", 10.0), setting("Instant Attack", true)),
                                module("Critical", true),
                                module("NoFall", true),
                                module("NoSlow", true, setting("Mode", "NoSlow"))))));
        return Collections.unmodifiableMap(presets);
    }

    private static Map<String, PresetLevel> levels(PresetLevel... levels) {
        Map<String, PresetLevel> result = new LinkedHashMap<>();
        for (PresetLevel level : levels) {
            result.put(level.id(), level);
        }
        return Collections.unmodifiableMap(result);
    }

    private static PresetLevel level(String id, String name, String description, ModulePreset... modules) {
        return new PresetLevel(id, name, description, List.of(modules));
    }

    private static ModulePreset module(String name, boolean enabled, SettingValue... settings) {
        return new ModulePreset(name, enabled, List.of(settings));
    }

    private static SettingValue setting(String name, Object value) {
        return new SettingValue(name, value);
    }

    public record GamePreset(String id, String name, String description, Map<String, PresetLevel> levels) {
    }

    public record PresetLevel(String id, String name, String description, List<ModulePreset> modules) {
    }

    private record ModulePreset(String module, boolean enabled, List<SettingValue> settings) {
    }

    private record SettingValue(String name, Object value) {
    }
}
