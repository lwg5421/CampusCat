package com.example.campuscat.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campuscat.R;
import com.example.campuscat.domain.CatSkin;
import com.example.campuscat.domain.CatStage;

/**
 * 여러 겹의 벡터 이미지를 쌓아서 고양이를 그린다.
 * 몸통(스킨 색으로 tint) → 줄무늬 → 얼굴 → 진화 단계별 소품 순서.
 */
public class CatView extends FrameLayout {

    private final ImageView body;
    private final ImageView stripes;
    private final ImageView accessory;

    public CatView(@NonNull Context context) {
        this(context, null);
    }

    public CatView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        body = addLayer(R.drawable.cat_body);
        stripes = addLayer(R.drawable.cat_stripes);
        addLayer(R.drawable.cat_face);
        accessory = addLayer(0);
        bind(CatStage.BABY, CatSkin.CHEESE);
    }

    private ImageView addLayer(@DrawableRes int drawable) {
        ImageView layer = new ImageView(getContext());
        layer.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layer.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (drawable != 0) {
            layer.setImageResource(drawable);
        }
        addView(layer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return layer;
    }

    public void bind(@NonNull CatStage stage, @NonNull CatSkin skin) {
        body.setColorFilter(skin.getFurColor(), PorterDuff.Mode.SRC_IN);
        stripes.setVisibility(skin.isStriped() ? VISIBLE : GONE);
        int accessoryRes = accessoryFor(stage);
        if (accessoryRes == 0) {
            accessory.setImageDrawable(null);
        } else {
            accessory.setImageResource(accessoryRes);
        }
        setContentDescription(stage.getDisplayName() + ", " + skin.getDisplayName());
    }

    @DrawableRes
    private static int accessoryFor(CatStage stage) {
        switch (stage) {
            case FRESHMAN:
                return R.drawable.cat_acc_bow;
            case HONOR:
                return R.drawable.cat_acc_glasses;
            case SCHOLAR:
                return R.drawable.cat_acc_scholar;
            case DOCTOR:
                return R.drawable.cat_acc_doctor;
            case BABY:
            default:
                return 0;
        }
    }

    /** 눌렀을 때 말랑하게 튀는 애니메이션. */
    public void bounce() {
        animate().cancel();
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight());
        animate()
                .scaleX(1.12f)
                .scaleY(0.88f)
                .setDuration(110)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(320)
                        .setInterpolator(new OvershootInterpolator(4f))
                        .start())
                .start();
    }
}
