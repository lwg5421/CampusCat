package com.example.campuscat.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campuscat.MainActivity;
import com.example.campuscat.R;
import com.example.campuscat.data.CatRepository;
import com.example.campuscat.domain.CatSkin;
import com.example.campuscat.domain.CatStage;
import com.example.campuscat.domain.LevelSystem;
import com.example.campuscat.domain.StudySession;
import com.example.campuscat.domain.StudyStats;
import com.example.campuscat.domain.TimeFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/** 홈: 수업 시작/종료 타이머와 오늘의 공부 현황. */
public class HomeFragment extends Fragment {

    /** 이보다 긴 수업은 종료 버튼을 깜빡한 것일 수 있어 한 번 더 확인한다. */
    private static final long LONG_SESSION_MILLIS = 6 * 60 * 60 * 1000L;
    private static final long TICK_MILLIS = 1000L;

    private static final long BUBBLE_HOLD_MILLIS = 3000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateLiveViews();
            handler.postDelayed(this, TICK_MILLIS);
        }
    };

    private CatRepository repository;

    private CatView catView;
    private TextView greetingText;
    private TextView bubbleText;
    private TextView levelText;
    private LinearProgressIndicator xpProgress;
    private TextView statusText;
    private TextView timerText;
    private TextView startedAtText;
    private TextInputLayout subjectLayout;
    private TextInputEditText subjectInput;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private TextView todayGoalText;
    private TextView todayTotalText;
    private LinearProgressIndicator todayProgress;
    private TextView todayHintText;

    /** 진행 중인 수업을 뺀 오늘의 공부 시간(분). */
    private long todayFinishedMinutes;
    /** 이 시각까지는 말풍선을 상태 문구로 덮어쓰지 않는다. */
    private long bubbleHoldUntil;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = CatRepository.get(requireContext());

        catView = view.findViewById(R.id.home_cat);
        greetingText = view.findViewById(R.id.home_greeting);
        bubbleText = view.findViewById(R.id.home_bubble);
        levelText = view.findViewById(R.id.home_level);
        xpProgress = view.findViewById(R.id.home_xp_progress);
        statusText = view.findViewById(R.id.home_status);
        timerText = view.findViewById(R.id.home_timer);
        startedAtText = view.findViewById(R.id.home_started_at);
        subjectLayout = view.findViewById(R.id.home_subject_layout);
        subjectInput = view.findViewById(R.id.home_subject_input);
        startButton = view.findViewById(R.id.home_start_button);
        stopButton = view.findViewById(R.id.home_stop_button);
        todayGoalText = view.findViewById(R.id.home_today_goal);
        todayTotalText = view.findViewById(R.id.home_today_total);
        todayProgress = view.findViewById(R.id.home_today_progress);
        todayHintText = view.findViewById(R.id.home_today_hint);

        startButton.setOnClickListener(v -> startSession());
        stopButton.setOnClickListener(v -> stopSession());
        catView.setOnClickListener(v -> {
            catView.bounce();
            String[] meows = getResources().getStringArray(R.array.cat_meows);
            say(meows[random.nextInt(meows.length)]);
        });
        subjectInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                v.clearFocus();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 타이머와 말풍선 복귀 콜백 모두. 화면을 떠난 뒤 getString() 이 불리지 않도록.
        handler.removeCallbacksAndMessages(null);
    }

    private void refresh() {
        int totalXp = repository.getTotalXp();
        int level = LevelSystem.levelForXp(totalXp);
        CatStage stage = CatStage.forLevel(level);
        CatSkin skin = repository.getSkin();
        String name = repository.getCatName();

        catView.bind(stage, skin);
        greetingText.setText(getString(R.string.home_greeting, name));
        if (LevelSystem.isMaxLevel(level)) {
            levelText.setText(getString(R.string.home_level_summary_max, level, stage.getDisplayName()));
        } else {
            levelText.setText(getString(R.string.home_level_summary, level, stage.getDisplayName(),
                    LevelSystem.xpRemaining(totalXp)));
        }
        xpProgress.setMax(LevelSystem.xpNeededForNextLevel(level));
        xpProgress.setProgressCompat(LevelSystem.xpIntoLevel(totalXp), false);

        boolean active = repository.isSessionActive();
        startButton.setVisibility(active ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(active ? View.VISIBLE : View.GONE);
        subjectLayout.setEnabled(!active);
        startedAtText.setVisibility(active ? View.VISIBLE : View.GONE);
        if (active) {
            String subject = repository.getActiveSubject();
            subjectInput.setText(subject);
            statusText.setText(subject.isEmpty()
                    ? getString(R.string.home_status_active)
                    : getString(R.string.home_status_active_subject, subject));
        } else {
            statusText.setText(R.string.home_status_idle);
        }

        TimeZone timeZone = TimeZone.getDefault();
        todayFinishedMinutes = StudyStats.minutesOnDay(repository.getSessions(), System.currentTimeMillis(), timeZone);
        todayGoalText.setText(getString(R.string.home_today_goal,
                TimeFormatter.formatDuration(repository.getDailyGoalMinutes())));

        handler.removeCallbacks(ticker);
        updateLiveViews();
        if (active) {
            handler.postDelayed(ticker, TICK_MILLIS);
        }
    }

    /** 1초마다 바뀌는 부분: 타이머, 쌓이는 XP, 오늘 공부 시간. */
    private void updateLiveViews() {
        long now = System.currentTimeMillis();
        boolean active = repository.isSessionActive();
        long todayMinutes = todayFinishedMinutes;
        if (active) {
            long start = repository.getActiveStartMillis();
            long elapsed = Math.max(0, now - start);
            timerText.setText(TimeFormatter.formatClock(elapsed));
            startedAtText.setText(getString(R.string.home_started_at, formatTime(start),
                    LevelSystem.xpForMinutes(elapsed / 60_000L)));
            List<StudySession> running = Collections.singletonList(new StudySession(start, now, ""));
            todayMinutes += StudyStats.minutesOnDay(running, now, TimeZone.getDefault());
        } else {
            timerText.setText(TimeFormatter.formatClock(0));
        }

        int goal = repository.getDailyGoalMinutes();
        todayTotalText.setText(TimeFormatter.formatDuration(todayMinutes));
        todayProgress.setMax(Math.max(1, goal));
        todayProgress.setProgressCompat((int) Math.min(goal, todayMinutes), false);
        boolean goalDone = todayMinutes >= goal;
        todayHintText.setText(goalDone
                ? getString(R.string.home_today_done)
                : getString(R.string.home_today_remaining, TimeFormatter.formatDuration(goal - todayMinutes)));

        if (now < bubbleHoldUntil) {
            return;
        }
        if (active) {
            bubbleText.setText(R.string.home_bubble_active);
        } else if (goalDone) {
            bubbleText.setText(R.string.home_bubble_goal_done);
        } else {
            bubbleText.setText(R.string.home_bubble_idle);
        }
    }

    private void startSession() {
        Editable text = subjectInput.getText();
        String subject = text == null ? "" : text.toString().trim();
        repository.startSession(System.currentTimeMillis(), subject);
        hideKeyboard();
        subjectInput.clearFocus();
        refresh();
        say(getString(R.string.home_bubble_started));
        catView.bounce();
    }

    /** 고양이가 잠깐 동안 하는 말. */
    private void say(CharSequence line) {
        bubbleText.setText(line);
        bubbleHoldUntil = System.currentTimeMillis() + BUBBLE_HOLD_MILLIS;
        handler.postDelayed(this::updateLiveViews, BUBBLE_HOLD_MILLIS + 50);
    }

    private void stopSession() {
        if (!repository.isSessionActive()) {
            refresh();
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - repository.getActiveStartMillis();
        if (elapsed < LONG_SESSION_MILLIS) {
            finishSession(now);
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_long_title)
                .setMessage(getString(R.string.home_long_message, TimeFormatter.formatDuration(elapsed / 60_000L)))
                .setPositiveButton(R.string.home_long_save, (dialog, which) -> finishSession(now))
                .setNegativeButton(R.string.home_long_discard, (dialog, which) -> {
                    repository.cancelSession();
                    refresh();
                    showMessage(getString(R.string.home_discarded));
                })
                .setNeutralButton(R.string.home_long_continue, null)
                .show();
    }

    private void finishSession(long now) {
        int levelBefore = repository.getLevel();
        StudySession session = repository.finishSession(now);
        subjectInput.setText(null);
        refresh();
        if (session == null) {
            showMessage(getString(R.string.home_too_short));
            return;
        }
        int levelAfter = repository.getLevel();
        if (levelAfter > levelBefore) {
            showLevelUp(levelBefore, levelAfter);
        } else {
            showMessage(getString(R.string.home_session_saved, session.getXp(),
                    TimeFormatter.formatDuration(session.getDurationMinutes())));
        }
    }

    private void showLevelUp(int levelBefore, int levelAfter) {
        StringBuilder message = new StringBuilder(
                getString(R.string.level_up_message, repository.getCatName(), levelBefore, levelAfter));
        CatStage stageBefore = CatStage.forLevel(levelBefore);
        CatStage stageAfter = CatStage.forLevel(levelAfter);
        if (stageAfter != stageBefore) {
            message.append("\n\n").append(getString(R.string.level_up_evolved, stageAfter.getDisplayName()));
        }
        List<String> newSkins = new ArrayList<>();
        for (CatSkin skin : CatSkin.values()) {
            if (!skin.isUnlockedAt(levelBefore) && skin.isUnlockedAt(levelAfter)) {
                newSkins.add(skin.getDisplayName());
            }
        }
        if (!newSkins.isEmpty()) {
            message.append("\n").append(getString(R.string.level_up_skins, joinNames(newSkins)));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.level_up_title)
                .setMessage(message)
                .setPositiveButton(R.string.level_up_go_cat,
                        (dialog, which) -> ((MainActivity) requireActivity()).selectTab(R.id.nav_cat))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private static String joinNames(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(millis));
    }

    private void showMessage(String message) {
        View root = getView();
        if (root == null) {
            return;
        }
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(subjectInput.getWindowToken(), 0);
        }
    }
}
