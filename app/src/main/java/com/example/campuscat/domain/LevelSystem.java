package com.example.campuscat.domain;

/**
 * 경험치(XP)와 레벨 사이의 변환 규칙.
 *
 * <p>수업 1분 = 1 XP 이고, 레벨 L 에서 L+1 로 올라가려면 60 × L XP 가 필요하다.
 * 즉 Lv.1 → Lv.2 는 1시간, Lv.2 → Lv.3 은 2시간 … 처럼 레벨이 오를수록 조금씩 더 오래 걸린다.
 */
public final class LevelSystem {

    public static final int XP_PER_MINUTE = 1;
    public static final int MAX_LEVEL = 30;

    private static final int XP_STEP = 60;

    private LevelSystem() {
    }

    /** 해당 레벨에 도달하기 위해 필요한 누적 XP. Lv.1 은 0 XP. */
    public static int totalXpForLevel(int level) {
        int clamped = Math.max(1, Math.min(level, MAX_LEVEL));
        return XP_STEP * clamped * (clamped - 1) / 2;
    }

    public static int levelForXp(int totalXp) {
        int level = 1;
        while (level < MAX_LEVEL && totalXp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    public static boolean isMaxLevel(int level) {
        return level >= MAX_LEVEL;
    }

    /** 현재 레벨 안에서 쌓은 XP. 최대 레벨이면 다음 레벨 요구량과 같다. */
    public static int xpIntoLevel(int totalXp) {
        int level = levelForXp(totalXp);
        if (isMaxLevel(level)) {
            return xpNeededForNextLevel(level);
        }
        return Math.max(0, totalXp - totalXpForLevel(level));
    }

    /** 현재 레벨에서 다음 레벨까지 필요한 XP 총량. */
    public static int xpNeededForNextLevel(int level) {
        int clamped = Math.max(1, Math.min(level, MAX_LEVEL));
        return XP_STEP * clamped;
    }

    /** 다음 레벨까지 남은 XP. 최대 레벨이면 0. */
    public static int xpRemaining(int totalXp) {
        int level = levelForXp(totalXp);
        if (isMaxLevel(level)) {
            return 0;
        }
        return totalXpForLevel(level + 1) - totalXp;
    }

    public static int xpForMinutes(long minutes) {
        long xp = Math.max(0, minutes) * XP_PER_MINUTE;
        return (int) Math.min(Integer.MAX_VALUE, xp);
    }
}
