package com.example.contentful_javasilver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.adapter.HistoryAdapter;
import com.example.contentful_javasilver.data.QuizHistory;
import com.example.contentful_javasilver.viewmodels.HistoryViewModel;
import java.util.ArrayList;
import java.util.List;

public class LearningLogFragment extends Fragment {

    private static final String TAG = "LearningLogFragment";
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryViewModel historyViewModel;
    private LinearLayout emptyStateContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_learning_log, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_learning_log);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        historyViewModel.getAllHistory().observe(getViewLifecycleOwner(), new Observer<List<QuizHistory>>() {
            @Override
            public void onChanged(List<QuizHistory> quizHistories) {
                if (quizHistories != null && !quizHistories.isEmpty()) {
                    Log.d(TAG, "History updated with " + quizHistories.size() + " entries.");
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateContainer.setVisibility(View.GONE);
                    adapter.submitList(quizHistories);
                } else {
                    Log.d(TAG, "History is null or empty.");
                    recyclerView.setVisibility(View.GONE);
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    adapter.submitList(new ArrayList<>());
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
