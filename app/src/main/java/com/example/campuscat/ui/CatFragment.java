package com.example.campuscat.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campuscat.R;
import com.example.campuscat.data.CatRepository;
import com.example.campuscat.domain.CatSkin;
import com.example.campuscat.domain.CatStage;
import com.example.campuscat.domain.LevelSystem;
import com.example.campuscat.domain.TimeFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Random;

/** 고양이: 레벨과 경험치, 진화 단계, 털 색 고르기. */
public class CatFragment extends Fragment {

    private static final long MEOW_VISIBLE_MILLIS = 1800L;

    private final Random random = new Random();

    private CatRepository repository;
    private CatView catView;
    private TextView meowText;
    private TextView nameText;
    private TextView stageLabel;
    private TextView xpValueText;
    private LinearProgressIndicator xpProgress;
    private TextView xpHintText;
    private TextView stageDescText;
    private TextView nextStageText;
    private LinearLayout stageRow;
    private ChipGroup skinGroup;

    public CatFragment() {
        super(R.layout.fragment_cat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = CatRepository.get(requireContext());
        catView = view.findViewById(R.id.cat_big);
        meowText = view.findViewById(R.id.cat_meow);
        nameText = view.findViewById(R.id.cat_name);
        stageLabel = view.findViewById(R.id.cat_stage_label);
        xpValueText = view.findViewById(R.id.cat_xp_value);
        xpProgress = view.findViewById(R.id.cat_xp_progress);
        xpHintText = view.findViewById(R.id.cat_xp_hint);
        stageDescText = view.findViewById(R.id.cat_stage_desc);
        nextStageText = view.findViewById(R.id.cat_next_stage);
        stageRow = view.findViewById(R.id.cat_stage_row);
        skinGroup = view.findViewById(R.id.cat_skin_group);

        catView.setOnClickListener(v -> {
            catView.bounce();
            meow();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_cat);
        refresh();
    }

    @Override
    public void onDestroyView() {
        meowText.animate().cancel();
        super.onDestroyView();
    }

    private void refresh() {
        int totalXp = repository.getTotalXp();
        int level = LevelSystem.levelForXp(totalXp);
        CatStage stage = CatStage.forLevel(level);
        CatSkin skin = repository.getSkin();

        catView.bind(stage, skin);
        nameText.setText(repository.getCatName());
        stageLabel.setText(getString(R.string.cat_stage_level, level, stage.getDisplayName()));

        int needed = LevelSystem.xpNeededForNextLevel(level);
        int into = LevelSystem.xpIntoLevel(totalXp);
        xpValueText.setText(getString(R.string.cat_xp_value, into, needed));
        xpProgress.setMax(needed);
        xpProgress.setProgressCompat(into, false);
        if (LevelSystem.isMaxLevel(level)) {
            xpHintText.setText(R.string.cat_xp_hint_max);
        } else {
            long minutesLeft = LevelSystem.xpRemaining(totalXp) / LevelSystem.XP_PER_MINUTE;
            xpHintText.setText(getString(R.string.cat_xp_hint, TimeFormatter.formatDuration(minutesLeft)));
        }

        stageDescText.setText(stage.getDescription());
        CatStage next = stage.next();
        if (next == null) {
            nextStageText.setText(R.string.cat_last_stage);
        } else {
            nextStageText.setText(getString(R.string.cat_next_stage, next.getMinLevel(), next.getDisplayName()));
        }

        bindStages(stage, skin, level);
        bindSkins(skin, level);
    }

    private void bindStages(CatStage current, CatSkin skin, int level) {
        stageRow.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (CatStage stage : CatStage.values()) {
            View item = inflater.inflate(R.layout.item_stage, stageRow, false);
            CatView cat = item.findViewById(R.id.stage_cat);
            TextView name = item.findViewById(R.id.stage_name);
            TextView unlock = item.findViewById(R.id.stage_level);
            cat.bind(stage, skin);
            name.setText(stage.getDisplayName());
            unlock.setText(getString(R.string.cat_stage_unlock, stage.getMinLevel()));
            if (stage == current) {
                item.setBackgroundResource(R.drawable.bg_stage_current);
            }
            item.setAlpha(level >= stage.getMinLevel() ? 1f : 0.35f);
            stageRow.addView(item);
        }
    }

    private void bindSkins(CatSkin selected, int level) {
        skinGroup.setOnCheckedStateChangeListener(null);
        skinGroup.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (CatSkin skin : CatSkin.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_skin_chip, skinGroup, false);
            chip.setId(View.generateViewId());
            chip.setTag(skin);
            chip.setChipIcon(colorDot(skin.getFurColor()));
            boolean unlocked = skin.isUnlockedAt(level);
            chip.setText(unlocked
                    ? skin.getDisplayName()
                    : getString(R.string.cat_skin_locked, skin.getDisplayName(), skin.getUnlockLevel()));
            chip.setEnabled(unlocked);
            skinGroup.addView(chip);
            if (skin == selected) {
                chip.setChecked(true);
            }
        }
        skinGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null && chip.getTag() instanceof CatSkin) {
                onSkinSelected((CatSkin) chip.getTag(), level);
            }
        });
    }

    /** 칩 목록은 그대로 두고 고양이 그림만 다시 그린다. */
    private void onSkinSelected(CatSkin skin, int level) {
        repository.setSkin(skin);
        CatStage stage = CatStage.forLevel(level);
        catView.bind(stage, skin);
        bindStages(stage, skin, level);
        catView.bounce();
    }

    private GradientDrawable colorDot(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        int stroke = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics()));
        dot.setStroke(stroke, 0x55000000);
        return dot;
    }

    private void meow() {
        String[] meows = getResources().getStringArray(R.array.cat_meows);
        meowText.setText(meows[random.nextInt(meows.length)]);
        meowText.animate().cancel();
        meowText.setAlpha(0f);
        meowText.setVisibility(View.VISIBLE);
        meowText.animate()
                .alpha(1f)
                .setStartDelay(0)
                .setDuration(150)
                .withEndAction(() -> meowText.animate()
                        .alpha(0f)
                        .setStartDelay(MEOW_VISIBLE_MILLIS)
                        .setDuration(300)
                        .withEndAction(() -> meowText.setVisibility(View.INVISIBLE))
                        .start())
                .start();
    }
}
