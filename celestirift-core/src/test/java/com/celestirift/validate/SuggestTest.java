package com.celestirift.validate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuggestTest {

    @Test
    void suggestsCloseTypos() {
        assertEquals("radius", Suggest.closest("radus", List.of("radius", "points", "plane")));
        assertEquals("projectile", Suggest.closest("projectle", List.of("projectile", "particle")));
        assertEquals("cooldown", Suggest.closest("Cooldwn", List.of("cooldown", "mana", "skills")));
    }

    @Test
    void noSuggestionWhenNothingIsClose() {
        assertNull(Suggest.closest("banana", List.of("radius", "points")));
    }
}
