package com.example.campuscat.domain;

/** 고양이 털 색(스킨). 레벨이 오르면 하나씩 해금된다. */
public enum CatSkin {
    CHEESE("cheese", "치즈냥", 0xFFF2A65A, true, 1),
    WHITE("white", "흰냥", 0xFFFBF7F2, false, 1),
    MACKEREL("mackerel", "고등어냥", 0xFFA4A9AE, true, 3),
    BLACK("black", "까망냥", 0xFF45403C, false, 6),
    CREAM("cream", "크림냥", 0xFFF1D7B0, true, 10),
    BLUE("blue", "러블냥", 0xFF8E9DB3, false, 15);

    private final String id;
    private final String displayName;
    private final int furColor;
    private final boolean striped;
    private final int unlockLevel;

    CatSkin(String id, String displayName, int furColor, boolean striped, int unlockLevel) {
        this.id = id;
        this.displayName = displayName;
        this.furColor = furColor;
        this.striped = striped;
        this.unlockLevel = unlockLevel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** ARGB 색상 값. */
    public int getFurColor() {
        return furColor;
    }

    public boolean isStriped() {
        return striped;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public boolean isUnlockedAt(int level) {
        return level >= unlockLevel;
    }

    public static CatSkin fromId(String id) {
        for (CatSkin skin : values()) {
            if (skin.id.equals(id)) {
                return skin;
            }
        }
        return CHEESE;
    }
}
