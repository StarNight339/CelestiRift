package com.celestirift.api.skill;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** The resolved output of a targeter: entities and/or locations to act on. */
public final class SkillTargets {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Location> locations = new ArrayList<>();

    public SkillTargets addEntity(Entity e) {
        if (e != null) {
            entities.add(e);
        }
        return this;
    }

    public SkillTargets addLocation(Location l) {
        if (l != null) {
            locations.add(l.clone());
        }
        return this;
    }

    public List<Entity> entities() { return entities; }

    /** Locations, falling back to the locations of any targeted entities. */
    public List<Location> locations() {
        if (!locations.isEmpty()) {
            return locations;
        }
        List<Location> out = new ArrayList<>();
        for (Entity e : entities) {
            out.add(e.getLocation());
        }
        return out;
    }

    public boolean isEmpty() {
        return entities.isEmpty() && locations.isEmpty();
    }
}
