package com.example.campuscat.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.campuscat.BuildConfig;
import com.example.campuscat.R;
import com.example.campuscat.data.CatRepository;
import com.example.campuscat.domain.ReminderTime;
import com.example.campuscat.domain.TimeFormatter;
import com.example.campuscat.reminder.ReminderScheduler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/** 설정: 고양이 이름, 하루 목표, 수업 알림, 데이터 초기화. */
public class SettingsFragment extends Fragment {

    private static final String TIME_PICKER_TAG = "reminder_time_picker";

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableReminder();
                } else {
                    bindReminder();
                    showMessage(getString(R.string.settings_reminder_denied));
                }
            });

    private CatRepository repository;
    private TextView nameValue;
    private TextView goalValue;
    private Slider goalSlider;
    private MaterialSwitch reminderSwitch;
    private View reminderTimeRow;
    private TextView reminderTimeValue;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = CatRepository.get(requireContext());
        nameValue = view.findViewById(R.id.settings_name_value);
        goalValue = view.findViewById(R.id.settings_goal_value);
        goalSlider = view.findViewById(R.id.settings_goal_slider);
        reminderSwitch = view.findViewById(R.id.settings_reminder_switch);
        reminderTimeRow = view.findViewById(R.id.settings_reminder_time_row);
        reminderTimeValue = view.findViewById(R.id.settings_reminder_time_value);
        TextView versionValue = view.findViewById(R.id.settings_version_value);

        view.findViewById(R.id.settings_name_row).setOnClickListener(v -> showNameDialog());
        view.findViewById(R.id.settings_reset_row).setOnClickListener(v -> confirmReset());
        reminderTimeRow.setOnClickListener(v -> showTimePicker());
        versionValue.setText(BuildConfig.VERSION_NAME);

        goalSlider.setLabelFormatter(value -> TimeFormatter.formatDuration((long) value));
        goalSlider.addOnChangeListener((slider, value, fromUser) -> {
            goalValue.setText(TimeFormatter.formatDuration((long) value));
            if (fromUser) {
                repository.setDailyGoalMinutes((int) value);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_settings);
        nameValue.setText(repository.getCatName());
        bindGoal();
        bindReminder();
    }

    private void bindGoal() {
        float from = goalSlider.getValueFrom();
        float to = goalSlider.getValueTo();
        float step = goalSlider.getStepSize();
        // Slider 는 valueFrom + stepSize 의 배수만 받으므로 저장된 값을 맞춰 준다.
        float value = Math.max(from, Math.min(to, repository.getDailyGoalMinutes()));
        value = from + Math.round((value - from) / step) * step;
        goalSlider.setValue(value);
        goalValue.setText(TimeFormatter.formatDuration((long) value));
    }

    private void bindReminder() {
        boolean enabled = repository.isReminderEnabled() && canPostNotifications();
        reminderSwitch.setOnCheckedChangeListener(null);
        reminderSwitch.setChecked(enabled);
        reminderSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                requestEnableReminder();
            } else {
                repository.setReminderEnabled(false);
                ReminderScheduler.cancel(requireContext());
                bindReminder();
            }
        });
        reminderTimeRow.setEnabled(enabled);
        reminderTimeRow.setAlpha(enabled ? 1f : 0.5f);
        reminderTimeValue.setText(ReminderTime.format(repository.getReminderHour(), repository.getReminderMinute()));
    }

    private boolean canPostNotifications() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestEnableReminder() {
        if (canPostNotifications()) {
            enableReminder();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void enableReminder() {
        repository.setReminderEnabled(true);
        ReminderScheduler.schedule(requireContext());
        bindReminder();
        showMessage(getString(R.string.settings_reminder_on,
                ReminderTime.format(repository.getReminderHour(), repository.getReminderMinute())));
    }

    private void showTimePicker() {
        if (getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG) != null) {
            return;
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(repository.getReminderHour())
                .setMinute(repository.getReminderMinute())
                .setTitleText(R.string.settings_reminder_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            repository.setReminderTime(picker.getHour(), picker.getMinute());
            if (repository.isReminderEnabled()) {
                ReminderScheduler.schedule(requireContext());
                showMessage(getString(R.string.settings_reminder_on,
                        ReminderTime.format(picker.getHour(), picker.getMinute())));
            }
            bindReminder();
        });
        picker.show(getChildFragmentManager(), TIME_PICKER_TAG);
    }

    private void showNameDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_cat_name, null);
        TextInputEditText input = content.findViewById(R.id.dialog_name_input);
        input.setText(repository.getCatName());
        Editable current = input.getText();
        input.setSelection(current == null ? 0 : current.length());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_name_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Editable text = input.getText();
                    String name = text == null ? "" : text.toString().trim();
                    if (!name.isEmpty()) {
                        repository.setCatName(name);
                        nameValue.setText(name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setPositiveButton(R.string.settings_reset_confirm, (dialog, which) -> {
                    repository.clearAllRecords();
                    showMessage(getString(R.string.settings_reset_done));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
}
