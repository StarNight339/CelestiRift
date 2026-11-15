package com.celestirift.gem;

import com.celestirift.CelestiRift;
import com.celestirift.skill.load.ContentFiles;
import com.celestirift.trigger.TriggerType;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.Suggest;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads gem fusion recipes from fusions.yml and matches a set of gem ids to a
 * recipe during socketing.
 */
public final class FusionManager implements com.celestirift.util.Restorable {

    private final CelestiRift plugin;
    private final List<FusionRecipe> recipes = new ArrayList<>();

    public FusionManager(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @Override
    public Runnable snapshot() {
        var copy = new ArrayList<>(recipes);
        return () -> { recipes.clear(); recipes.addAll(copy); };
    }

    public int reload(Diagnostics diag) {
        recipes.clear();
        for (ContentFiles.File file : ContentFiles.load(plugin, "fusions.yml", "fusions", diag)) {
            load(file, diag);
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " fusion recipe(s).");
        return recipes.size();
    }

    private void load(ContentFiles.File file, Diagnostics diag) {
        var yaml = file.yaml();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection sec = yaml.getConfigurationSection(id);
            int line = file.lines().key(id);
            if (sec == null) {
                diag.warn(file.path(), line, id, "is not a fusion recipe (expected a block)");
                continue;
            }
            List<String> ingredients = sec.getStringList("ingredients");
            String resultGem = sec.getString("result-gem", "");
            String skill = sec.getString("skill", "");
            if (ingredients.size() < 2 || (skill.isBlank() && resultGem.isBlank())) {
                diag.error(file.path(), line, id,
                        "needs at least 2 ingredients and a 'skill' or 'result-gem' - skipped");
                continue;
            }
            List<String> lower = new ArrayList<>();
            for (String ing : ingredients) {
                lower.add(ing.toLowerCase(Locale.ROOT));
                if (plugin.gemDefs().get(ing).isEmpty()) {
                    diag.warn(file.path(), file.lines().find(id, ing), id, "ingredient '" + ing
                            + "' is not a gem in gems.yml", Suggest.closest(ing, plugin.gemDefs().ids()));
                }
            }
            if (!resultGem.isBlank() && plugin.gemDefs().get(resultGem).isEmpty()) {
                diag.warn(file.path(), file.lines().find(id, "result-gem"), id, "result-gem '" + resultGem
                        + "' is not defined", Suggest.closest(resultGem, plugin.gemDefs().ids()));
            } else if (resultGem.isBlank() && plugin.skills().getSkill(skill).isEmpty()) {
                diag.warn(file.path(), file.lines().find(id, "skill"), id, "uses unknown skill '" + skill + "'",
                        Suggest.closest(skill, plugin.skills().skillNames()));
            }
            TriggerType trigger = TriggerType.fromString(sec.getString("trigger", "RIGHT_CLICK"));
            if (trigger == null) {
                trigger = TriggerType.RIGHT_CLICK;
            }
            int tier = Math.max(1, Math.min(5, sec.getInt("tier", 5)));
            recipes.add(new FusionRecipe(id, lower, resultGem, skill, trigger, tier));
        }
    }

    /**
     * Find a recipe whose ingredients are all present in {@code presentGemIds}
     * (case-insensitive, each ingredient consumed once). Returns null if none.
     */
    public @Nullable FusionRecipe match(List<String> presentGemIds) {
        List<String> pool = new ArrayList<>();
        for (String s : presentGemIds) {
            if (s != null && !s.isBlank()) {
                pool.add(s.toLowerCase(Locale.ROOT));
            }
        }
        for (FusionRecipe recipe : recipes) {
            List<String> remaining = new ArrayList<>(pool);
            boolean ok = true;
            for (String ing : recipe.ingredients()) {
                if (!remaining.remove(ing)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return recipe;
            }
        }
        return null;
    }

    public List<FusionRecipe> recipes() {
        return recipes;
    }
}
