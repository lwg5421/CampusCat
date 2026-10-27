package com.example.campuscat.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campuscat.domain.CatSkin;
import com.example.campuscat.domain.LevelSystem;
import com.example.campuscat.domain.StudySession;
import com.example.campuscat.domain.StudyStats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 고양이 상태와 수업 기록을 SharedPreferences 에 저장한다.
 * 경험치는 따로 저장하지 않고 수업 기록에서 계산하므로, 기록을 지우면 경험치도 함께 줄어든다.
 */
public final class CatRepository {

    private static final String TAG = "CatRepository";
    private static final String PREFS_NAME = "campuscat";

    private static final String KEY_CAT_NAME = "cat_name";
    private static final String KEY_SKIN = "skin";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_ACTIVE_START = "active_start";
    private static final String KEY_ACTIVE_SUBJECT = "active_subject";
    private static final String KEY_DAILY_GOAL = "daily_goal_minutes";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    public static final String DEFAULT_CAT_NAME = "냥이";
    public static final int DEFAULT_DAILY_GOAL_MINUTES = 120;
    public static final long MIN_SESSION_MILLIS = 60_000L;

    private static CatRepository instance;

    private final SharedPreferences prefs;
    /** 최신 기록이 앞에 오도록 정렬된 캐시. */
    @Nullable
    private List<StudySession> sessionsCache;

    private CatRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized CatRepository get(@NonNull Context context) {
        if (instance == null) {
            instance = new CatRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ---- 고양이 ----

    @NonNull
    public String getCatName() {
        String name = prefs.getString(KEY_CAT_NAME, DEFAULT_CAT_NAME);
        return name == null || name.trim().isEmpty() ? DEFAULT_CAT_NAME : name;
    }

    public void setCatName(@NonNull String name) {
        prefs.edit().putString(KEY_CAT_NAME, name.trim()).apply();
    }

    /** 선택한 스킨. 기록 삭제 등으로 잠긴 스킨이 되면 기본 스킨으로 돌아간다. */
    @NonNull
    public CatSkin getSkin() {
        CatSkin skin = CatSkin.fromId(prefs.getString(KEY_SKIN, CatSkin.CHEESE.getId()));
        return skin.isUnlockedAt(getLevel()) ? skin : CatSkin.CHEESE;
    }

    public void setSkin(@NonNull CatSkin skin) {
        prefs.edit().putString(KEY_SKIN, skin.getId()).apply();
    }

    public int getTotalXp() {
        return StudyStats.totalXp(getSessions());
    }

    public int getLevel() {
        return LevelSystem.levelForXp(getTotalXp());
    }

    // ---- 수업 기록 ----

    @NonNull
    public synchronized List<StudySession> getSessions() {
        if (sessionsCache == null) {
            sessionsCache = loadSessions();
        }
        return Collections.unmodifiableList(sessionsCache);
    }

    public boolean isSessionActive() {
        return prefs.getLong(KEY_ACTIVE_START, 0L) > 0L;
    }

    public long getActiveStartMillis() {
        return prefs.getLong(KEY_ACTIVE_START, 0L);
    }

    @NonNull
    public String getActiveSubject() {
        String subject = prefs.getString(KEY_ACTIVE_SUBJECT, "");
        return subject == null ? "" : subject;
    }

    public void startSession(long nowMillis, @Nullable String subject) {
        prefs.edit()
                .putLong(KEY_ACTIVE_START, nowMillis)
                .putString(KEY_ACTIVE_SUBJECT, subject == null ? "" : subject.trim())
                .apply();
    }

    /**
     * 진행 중인 수업을 끝내고 기록한다.
     *
     * @return 저장된 기록. 진행 중인 수업이 없거나 1분 미만이면 저장하지 않고 null.
     */
    @Nullable
    public synchronized StudySession finishSession(long nowMillis) {
        long start = getActiveStartMillis();
        if (start <= 0L) {
            return null;
        }
        String subject = getActiveSubject();
        clearActiveSession();
        if (nowMillis - start < MIN_SESSION_MILLIS) {
            return null;
        }
        StudySession session = new StudySession(start, nowMillis, subject);
        List<StudySession> updated = new ArrayList<>(getSessions());
        updated.add(0, session);
        saveSessions(updated);
        return session;
    }

    /** 진행 중인 수업을 기록하지 않고 버린다. */
    public void cancelSession() {
        clearActiveSession();
    }

    public synchronized void deleteSession(long startMillis) {
        List<StudySession> updated = new ArrayList<>(getSessions());
        Iterator<StudySession> iterator = updated.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStartMillis() == startMillis) {
                iterator.remove();
            }
        }
        saveSessions(updated);
    }

    /** 수업 기록과 진행 중인 수업을 모두 지운다. 이름과 설정은 유지한다. */
    public synchronized void clearAllRecords() {
        prefs.edit()
                .remove(KEY_SESSIONS)
                .remove(KEY_ACTIVE_START)
                .remove(KEY_ACTIVE_SUBJECT)
                .apply();
        sessionsCache = new ArrayList<>();
    }

    // ---- 설정 ----

    public int getDailyGoalMinutes() {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL_MINUTES);
    }

    public void setDailyGoalMinutes(int minutes) {
        prefs.edit().putInt(KEY_DAILY_GOAL, minutes).apply();
    }

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public void setReminderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 9);
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0);
    }

    public void setReminderTime(int hour, int minute) {
        prefs.edit()
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();
    }

    // ---- 내부 ----

    private void clearActiveSession() {
        prefs.edit()
                .remove(KEY_ACTIVE_START)
                .remove(KEY_ACTIVE_SUBJECT)
                .apply();
    }

    private List<StudySession> loadSessions() {
        List<StudySession> result = new ArrayList<>();
        String json = prefs.getString(KEY_SESSIONS, null);
        if (json == null) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new StudySession(item.getLong("s"), item.getLong("e"), item.optString("sub", "")));
            }
        } catch (JSONException e) {
            Log.e(TAG, "저장된 수업 기록을 읽지 못했어요", e);
        }
        Collections.sort(result, (a, b) -> Long.compare(b.getStartMillis(), a.getStartMillis()));
        return result;
    }

    private void saveSessions(List<StudySession> sessions) {
        JSONArray array = new JSONArray();
        try {
            for (StudySession session : sessions) {
                JSONObject item = new JSONObject();
                item.put("s", session.getStartMillis());
                item.put("e", session.getEndMillis());
                if (session.hasSubject()) {
                    item.put("sub", session.getSubject());
                }
                array.put(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "수업 기록을 저장하지 못했어요", e);
            return;
        }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply();
        sessionsCache = sessions;
    }
}
