package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@code stealitem{slot=hand;amount=1;material=DIAMOND;drop=false}} — takes an
 * item off each targeted entity and hands it to the caster.
 *
 * <p>{@code slot} is {@code hand}, {@code offhand}, {@code armor}, or
 * {@code random} (default — any non-empty slot of a player's inventory, or any
 * equipment slot of a mob). {@code amount=0} lifts the whole stack;
 * {@code material} only steals a matching item; {@code drop=true} knocks the
 * item to the ground at the victim's feet instead of pocketing it.
 *
 * <p>If the victim has nothing worth taking, the mechanic does nothing.
 */
public final class StealMechanic implements TargetedEntityMechanic {

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final String slot;
    private final int amount;
    private final @Nullable Material filter;
    private final boolean drop;

    public StealMechanic(ConfigMap cfg) {
        this.slot = cfg.getString("random", "slot", "s", "from").toLowerCase(Locale.ROOT);
        this.amount = cfg.getInt(1, "amount", "a", "count");
        this.filter = com.celestirift.util.Registries.itemMaterial(cfg.getString("", "material", "item", "type")).orElse(null);
        this.drop = cfg.getBoolean(false, "drop");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!com.celestirift.skill.CombatRules.canHarm(meta.caster(), target)) {
            return;
        }
        Slot source = pick(target);
        if (source == null) {
            return;
        }
        ItemStack stack = source.get().get();
        int take = (amount <= 0) ? stack.getAmount() : Math.min(amount, stack.getAmount());

        ItemStack stolen = stack.clone();
        stolen.setAmount(take);
        if (take >= stack.getAmount()) {
            source.set().accept(null);
        } else {
            stack.setAmount(stack.getAmount() - take);
            source.set().accept(stack);
        }
        deliver(meta, target, stolen);
    }

    /** Choose a slot to rob, honouring {@code slot=} and the material filter. */
    private @Nullable Slot pick(Entity target) {
        EntityEquipment equipment = (target instanceof LivingEntity le) ? le.getEquipment() : null;
        List<Slot> candidates = new ArrayList<>();

        switch (slot) {
            case "hand", "mainhand", "held" -> addEquipment(candidates, equipment, EquipmentSlot.HAND);
            case "offhand" -> addEquipment(candidates, equipment, EquipmentSlot.OFF_HAND);
            case "armor" -> {
                for (EquipmentSlot s : ARMOR) {
                    addEquipment(candidates, equipment, s);
                }
            }
            default -> {
                // random / any / inventory
                if (target instanceof Player player) {
                    addStorage(candidates, player.getInventory());
                } else {
                    addEquipment(candidates, equipment, EquipmentSlot.HAND);
                    addEquipment(candidates, equipment, EquipmentSlot.OFF_HAND);
                    for (EquipmentSlot s : ARMOR) {
                        addEquipment(candidates, equipment, s);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void addEquipment(List<Slot> out, @Nullable EntityEquipment equipment, EquipmentSlot slot) {
        if (equipment == null || !stealable(equipment.getItem(slot))) {
            return;
        }
        out.add(new Slot(() -> equipment.getItem(slot), item -> equipment.setItem(slot, item)));
    }

    private void addStorage(List<Slot> out, Inventory inventory) {
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (!stealable(contents[i])) {
                continue;
            }
            final int index = i;
            out.add(new Slot(() -> inventory.getItem(index), item -> inventory.setItem(index, item)));
        }
    }

    private boolean stealable(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return false;
        }
        return filter == null || item.getType() == filter;
    }

    /** Pocket the loot for the caster, or spill it at the victim's feet. */
    private void deliver(SkillMetadata meta, Entity victim, ItemStack stolen) {
        if (drop) {
            dropAt(victim.getLocation(), stolen);
            return;
        }
        if (meta.caster() instanceof Player thief) {
            Map<Integer, ItemStack> overflow = thief.getInventory().addItem(stolen);
            for (ItemStack rest : overflow.values()) {
                dropAt(thief.getLocation(), rest); // inventory full — don't void the item
            }
            return;
        }
        dropAt(meta.caster().getLocation(), stolen);
    }

    private static void dropAt(Location location, ItemStack item) {
        if (location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location.clone().add(0, 0.5, 0), item);
        }
    }

    /** A readable/writable item slot, wherever it lives. */
    private record Slot(Supplier<ItemStack> get, Consumer<ItemStack> set) {}
}
