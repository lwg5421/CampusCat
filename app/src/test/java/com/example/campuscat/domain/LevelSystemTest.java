package com.example.campuscat.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LevelSystemTest {

    @Test
    public void levelThresholdsGrowByOneHourPerLevel() {
        assertEquals(0, LevelSystem.totalXpForLevel(1));
        assertEquals(60, LevelSystem.totalXpForLevel(2));
        assertEquals(180, LevelSystem.totalXpForLevel(3));
        assertEquals(360, LevelSystem.totalXpForLevel(4));
        assertEquals(2700, LevelSystem.totalXpForLevel(10));
    }

    @Test
    public void levelForXpUsesThresholdsInclusively() {
        assertEquals(1, LevelSystem.levelForXp(0));
        assertEquals(1, LevelSystem.levelForXp(59));
        assertEquals(2, LevelSystem.levelForXp(60));
        assertEquals(2, LevelSystem.levelForXp(179));
        assertEquals(3, LevelSystem.levelForXp(180));
    }

    @Test
    public void progressWithinLevel() {
        assertEquals(30, LevelSystem.xpIntoLevel(90));
        assertEquals(120, LevelSystem.xpNeededForNextLevel(2));
        assertEquals(90, LevelSystem.xpRemaining(90));
    }

    @Test
    public void levelIsCappedAtMax() {
        int huge = LevelSystem.totalXpForLevel(LevelSystem.MAX_LEVEL) + 100_000;
        assertEquals(LevelSystem.MAX_LEVEL, LevelSystem.levelForXp(huge));
        assertTrue(LevelSystem.isMaxLevel(LevelSystem.levelForXp(huge)));
        assertEquals(0, LevelSystem.xpRemaining(huge));
        assertEquals(LevelSystem.xpNeededForNextLevel(LevelSystem.MAX_LEVEL), LevelSystem.xpIntoLevel(huge));
        assertFalse(LevelSystem.isMaxLevel(LevelSystem.MAX_LEVEL - 1));
    }

    @Test
    public void negativeMinutesGiveNoXp() {
        assertEquals(0, LevelSystem.xpForMinutes(-5));
        assertEquals(45, LevelSystem.xpForMinutes(45));
    }

    @Test
    public void stagesFollowLevels() {
        assertEquals(CatStage.BABY, CatStage.forLevel(1));
        assertEquals(CatStage.BABY, CatStage.forLevel(2));
        assertEquals(CatStage.FRESHMAN, CatStage.forLevel(3));
        assertEquals(CatStage.HONOR, CatStage.forLevel(9));
        assertEquals(CatStage.SCHOLAR, CatStage.forLevel(10));
        assertEquals(CatStage.DOCTOR, CatStage.forLevel(LevelSystem.MAX_LEVEL));
        assertEquals(CatStage.FRESHMAN, CatStage.BABY.next());
        assertEquals(null, CatStage.DOCTOR.next());
    }

    @Test
    public void skinsUnlockByLevelAndFallBackToCheese() {
        assertTrue(CatSkin.CHEESE.isUnlockedAt(1));
        assertFalse(CatSkin.BLACK.isUnlockedAt(5));
        assertTrue(CatSkin.BLACK.isUnlockedAt(6));
        assertEquals(CatSkin.MACKEREL, CatSkin.fromId("mackerel"));
        assertEquals(CatSkin.CHEESE, CatSkin.fromId("unknown"));
        assertEquals(CatSkin.CHEESE, CatSkin.fromId(null));
    }
}
