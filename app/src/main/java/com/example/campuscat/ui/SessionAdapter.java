package com.example.campuscat.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscat.R;
import com.example.campuscat.domain.StudySession;
import com.example.campuscat.domain.TimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 수업 기록 목록. 길게 누르면 삭제 콜백을 부른다. */
public class SessionAdapter extends ListAdapter<StudySession, SessionAdapter.ViewHolder> {

    public interface OnSessionLongClickListener {
        void onSessionLongClick(@NonNull StudySession session);
    }

    private static final DiffUtil.ItemCallback<StudySession> DIFF = new DiffUtil.ItemCallback<StudySession>() {
        @Override
        public boolean areItemsTheSame(@NonNull StudySession oldItem, @NonNull StudySession newItem) {
            return oldItem.getStartMillis() == newItem.getStartMillis();
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudySession oldItem, @NonNull StudySession newItem) {
            return oldItem.getEndMillis() == newItem.getEndMillis()
                    && oldItem.getSubject().equals(newItem.getSubject());
        }
    };

    private final OnSessionLongClickListener longClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 (E)", Locale.KOREA);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);

    public SessionAdapter(@NonNull OnSessionLongClickListener longClickListener) {
        super(DIFF);
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudySession session = getItem(position);
        View itemView = holder.itemView;
        holder.subject.setText(session.hasSubject()
                ? session.getSubject()
                : itemView.getContext().getString(R.string.records_default_subject));
        holder.time.setText(describeTime(session));
        holder.duration.setText(TimeFormatter.formatDuration(session.getDurationMinutes()));
        holder.xp.setText(itemView.getContext().getString(R.string.records_xp, session.getXp()));
        itemView.setOnLongClickListener(v -> {
            longClickListener.onSessionLongClick(session);
            return true;
        });
    }

    @NonNull
    String describeTime(@NonNull StudySession session) {
        Date start = new Date(session.getStartMillis());
        Date end = new Date(session.getEndMillis());
        return dateFormat.format(start) + " " + timeFormat.format(start) + " ~ " + timeFormat.format(end);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView subject;
        final TextView time;
        final TextView duration;
        final TextView xp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.session_subject);
            time = itemView.findViewById(R.id.session_time);
            duration = itemView.findViewById(R.id.session_duration);
            xp = itemView.findViewById(R.id.session_xp);
        }
    }
}
