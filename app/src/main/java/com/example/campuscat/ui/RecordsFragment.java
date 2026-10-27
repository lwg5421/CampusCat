package com.example.campuscat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscat.R;
import com.example.campuscat.data.CatRepository;
import com.example.campuscat.domain.StudySession;
import com.example.campuscat.domain.StudyStats;
import com.example.campuscat.domain.TimeFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** 기록: 요약 통계, 최근 7일 그래프, 수업 기록 목록. */
public class RecordsFragment extends Fragment {

    private static final int CHART_DAYS = 7;
    private static final String DAY_NAMES = "일월화수목금토";

    private CatRepository repository;
    private HeaderAdapter headerAdapter;
    private SessionAdapter sessionAdapter;

    public RecordsFragment() {
        super(R.layout.fragment_records);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = CatRepository.get(requireContext());
        headerAdapter = new HeaderAdapter();
        sessionAdapter = new SessionAdapter(this::confirmDelete);

        RecyclerView list = view.findViewById(R.id.records_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new ConcatAdapter(headerAdapter, sessionAdapter));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_records);
        refresh();
    }

    private void refresh() {
        List<StudySession> sessions = repository.getSessions();
        long now = System.currentTimeMillis();
        TimeZone timeZone = TimeZone.getDefault();

        long[] daily = StudyStats.dailyMinutes(sessions, now, CHART_DAYS, timeZone);
        long weekTotal = 0;
        for (long minutes : daily) {
            weekTotal += minutes;
        }

        HeaderData data = new HeaderData();
        data.weekText = TimeFormatter.formatDuration(weekTotal);
        data.streakText = getString(R.string.records_streak_value, StudyStats.streakDays(sessions, now, timeZone));
        data.totalText = TimeFormatter.formatDuration(StudyStats.totalMinutes(sessions));
        data.dailyMinutes = daily;
        data.dayLabels = dayLabels(now, timeZone);
        data.goalMinutes = repository.getDailyGoalMinutes();
        data.empty = sessions.isEmpty();
        headerAdapter.setData(data);

        sessionAdapter.submitList(new ArrayList<>(sessions));
    }

    private String[] dayLabels(long now, TimeZone timeZone) {
        String[] labels = new String[CHART_DAYS];
        long today = StudyStats.startOfDay(now, timeZone);
        Calendar calendar = Calendar.getInstance(timeZone);
        for (int i = 0; i < CHART_DAYS; i++) {
            if (i == CHART_DAYS - 1) {
                labels[i] = getString(R.string.records_today);
                continue;
            }
            calendar.setTimeInMillis(StudyStats.addDays(today, i - (CHART_DAYS - 1), timeZone));
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1 = 일요일
            labels[i] = String.valueOf(DAY_NAMES.charAt(dayOfWeek - 1));
        }
        return labels;
    }

    private void confirmDelete(@NonNull StudySession session) {
        String subject = session.hasSubject() ? session.getSubject() : getString(R.string.records_default_subject);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.records_delete_title)
                .setMessage(getString(R.string.records_delete_message, subject,
                        TimeFormatter.formatDuration(session.getDurationMinutes()), session.getXp()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deleteSession(session.getStartMillis());
                    refresh();
                    View root = getView();
                    if (root != null) {
                        Snackbar.make(root, R.string.records_deleted, Snackbar.LENGTH_SHORT)
                                .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                                .show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static final class HeaderData {
        String weekText;
        String streakText;
        String totalText;
        long[] dailyMinutes;
        String[] dayLabels;
        long goalMinutes;
        boolean empty;
    }

    /** 목록 맨 위에 붙는 요약 영역. 항상 한 칸만 있다. */
    private static final class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {

        @Nullable
        private HeaderData data;

        void setData(@NonNull HeaderData data) {
            this.data = data;
            notifyItemChanged(0);
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_records_header, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (data == null) {
                return;
            }
            holder.week.setText(data.weekText);
            holder.streak.setText(data.streakText);
            holder.total.setText(data.totalText);
            holder.chart.setData(data.dailyMinutes, data.dayLabels, data.goalMinutes);
            holder.empty.setVisibility(data.empty ? View.VISIBLE : View.GONE);
            holder.hint.setVisibility(data.empty ? View.GONE : View.VISIBLE);
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView week;
            final TextView streak;
            final TextView total;
            final WeekBarChartView chart;
            final TextView empty;
            final TextView hint;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                week = itemView.findViewById(R.id.stat_week_value);
                streak = itemView.findViewById(R.id.stat_streak_value);
                total = itemView.findViewById(R.id.stat_total_value);
                chart = itemView.findViewById(R.id.records_chart);
                empty = itemView.findViewById(R.id.records_empty);
                hint = itemView.findViewById(R.id.records_list_hint);
            }
        }
    }
}
