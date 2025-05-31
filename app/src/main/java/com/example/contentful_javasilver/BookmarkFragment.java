package com.example.contentful_javasilver;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.contentful_javasilver.adapter.ProblemListAdapter;
import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.viewmodels.BookmarkViewModel;

import java.util.ArrayList;
import java.util.List;

public class BookmarkFragment extends Fragment {

    private static final String TAG = "BookmarkFragment";
    private BookmarkViewModel bookmarkViewModel;
    private ProblemListAdapter adapter;
    private RecyclerView recyclerView;
    private NavController navController;
    private QuizDao quizDao;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuizDatabase db = QuizDatabase.getDatabase(requireActivity().getApplication());
        quizDao = db.quizDao();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmark, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.bookmark_recycler_view);
        navController = Navigation.findNavController(view);

        adapter = new ProblemListAdapter(quiz -> {
            String qid = quiz.getQid();

            if (qid == null || qid.isEmpty()) {
                Log.e(TAG, "Invalid QID for navigation from bookmark: " + qid);
                Toast.makeText(getContext(), "問題IDが無効なため遷移できません", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Navigating to QuizFragment with qid: " + qid);
            BookmarkFragmentDirections.ActionBookmarkFragmentToQuizFragment action =
                    BookmarkFragmentDirections.actionBookmarkFragmentToQuizFragment();
            
            Bundle args = new Bundle();
            args.putString("qid", qid);
            args.putBoolean("isRandomMode", false);
            
            try {
                 navController.navigate(action.getActionId(), args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to QuizFragment from Bookmark failed", e);
                Toast.makeText(getContext(), "画面遷移に失敗しました", Toast.LENGTH_SHORT).show();
            }
        }, quizDao);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        bookmarkViewModel = new ViewModelProvider(this).get(BookmarkViewModel.class);

        bookmarkViewModel.getBookmarkedQuizzes().observe(getViewLifecycleOwner(), quizzes -> {
            if (quizzes != null) {
                Log.d(TAG, "Observer received " + quizzes.size() + " bookmarked quizzes.");
            } else {
                Log.d(TAG, "Observer received null list.");
            }

            if (quizzes != null) {
                adapter.submitList(new ArrayList<>(quizzes));
                Log.d(TAG, "Submitted list to adapter.");
            } else {
                adapter.submitList(new ArrayList<>());
                Log.d(TAG, "Submitted empty list to adapter.");
            }
        });
    }
}
