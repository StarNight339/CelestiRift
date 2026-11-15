package com.celestirift.api.skill;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * A number attribute that may depend on the cast, e.g. {@code damage: "6 + <caster.level> * 0.5"}.
 * Read it with {@link ConfigMap#getNumber} in a mechanic's constructor and call
 * {@link #get} when the mechanic runs.
 */
public interface NumberValue {

    /**
     * @param meta   the running cast
     * @param target current target entity, if any (for {@code <target.*>})
     * @return the value for this cast
     */
    double get(SkillMetadata meta, @Nullable Entity target);

    /** @return true if {@link #get} always returns the same value */
    boolean isConstant();

    /**
     * @param value the fixed value
     * @return a constant number
     */
    static NumberValue constant(double value) {
        return new NumberValue() {
            @Override public double get(SkillMetadata meta, @Nullable Entity target) { return value; }
            @Override public boolean isConstant() { return true; }
            @Override public String toString() { return String.valueOf(value); }
        };
    }

    /**
     * @param expression compiled expression; variables resolve through {@link Variables}
     * @return a number evaluated per cast
     */
    static NumberValue of(Expression expression) {
        if (expression.isConstant()) {
            return constant(expression.eval(name -> 0));
        }
        return new NumberValue() {
            @Override
            public double get(SkillMetadata meta, @Nullable Entity target) {
                double d = expression.eval(name -> Variables.resolve(name, meta, target));
                return Double.isFinite(d) ? d : 0;
            }

            @Override public boolean isConstant() { return false; }
            @Override public String toString() { return expression.toString(); }
        };
    }
}
