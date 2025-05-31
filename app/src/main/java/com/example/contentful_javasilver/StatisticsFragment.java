package com.example.contentful_javasilver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Added import
import android.widget.LinearLayout; // Import LinearLayout

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController; // Added import
import androidx.navigation.Navigation; // Added import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.adapter.StatisticsAdapter;
import com.example.contentful_javasilver.viewmodels.StatisticsViewModel;
import com.example.contentful_javasilver.data.ProblemStats; // Import ProblemStats

public class StatisticsFragment extends Fragment {

    private StatisticsViewModel statisticsViewModel;
    private RecyclerView recyclerView;
    private StatisticsAdapter adapter;
    private LinearLayout emptyStateContainer; // Add reference for empty state

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModel
        statisticsViewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_statistics);
        emptyStateContainer = view.findViewById(R.id.empty_state_statistics_container); // Get reference
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true); // Optional optimization

        // Setup Adapter
        adapter = new StatisticsAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Removed Back Button setup code as it's handled by MainActivity's Toolbar

        // Observe the LiveData from the ViewModel
        statisticsViewModel.getProblemStatistics().observe(getViewLifecycleOwner(), problemStats -> {
            // Implement empty state logic
            if (problemStats != null && !problemStats.isEmpty()) {
                // Data exists: Show RecyclerView, hide empty state
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateContainer.setVisibility(View.GONE);
                adapter.submitList(problemStats);
            } else {
                // No data: Hide RecyclerView, show empty state
                recyclerView.setVisibility(View.GONE);
                emptyStateContainer.setVisibility(View.VISIBLE);
                adapter.submitList(null); // Submit null or empty list to clear adapter
            }
        });
    }
}
