package com.example.contentful_javasilver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.contentful_javasilver.data.QuizEntity
import com.example.contentful_javasilver.ui.QuizScreen
import com.example.contentful_javasilver.ui.theme.ContentfulJavasilverTheme
import com.example.contentful_javasilver.viewmodels.QuizViewModel

class QuizFragment : Fragment() {
    private lateinit var viewModel: QuizViewModel
    private var initialQid: String? = null
    private var isRandomMode = false
    private lateinit var menuHost: MenuHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Start")
        viewModel = ViewModelProvider(requireActivity())[QuizViewModel::class.java]
        Log.d(TAG, "onCreate: ViewModelProvider finished")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Start")
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            Log.d(TAG, "onCreateView: Setting content...")
            setContent {
                ContentfulJavasilverTheme {
                    QuizScreen(viewModel, isRandomMode)
                        }
            }
            Log.d(TAG, "onCreateView: Set content finished")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Start")

        menuHost = requireActivity()
        setupMenu()
        setupObservers()
        Log.d(TAG, "onViewCreated: Basic setup finished")

        Log.d(TAG, "onViewCreated: Checking arguments...")
        arguments?.let { bundle ->
            Log.d(TAG, "onViewCreated: arguments bundle is NOT null, processing...")
            try {
                Log.d(TAG, "onViewCreated: Calling QuizFragmentArgs.fromBundle()")
                val args = QuizFragmentArgs.fromBundle(bundle)
                Log.d(TAG, "onViewCreated: QuizFragmentArgs.fromBundle() successful")
                initialQid = args.qid
                isRandomMode = args.isRandomMode
                Log.d(TAG, "Arguments received - qid: $initialQid, isRandomMode: $isRandomMode")
            } catch (e: Exception) {
                 Log.e(TAG, "onViewCreated: Error parsing arguments from bundle", e)
            }
        } ?: run {
            Log.w(TAG, "onViewCreated: arguments bundle IS null")
        }

        Log.d(TAG, "onViewCreated: Arguments processed. InitialQid: $initialQid, isRandomMode: $isRandomMode")

        // Refined loading logic: Prioritize isRandomMode
        if (isRandomMode) {
            Log.d(TAG, "Random mode is ON, loading initial random quiz regardless of qid ($initialQid).")
            viewModel.loadRandomQuizId()
        } else if (initialQid?.isNotEmpty() == true) {
            Log.d(TAG, "Loading quiz by initial QID: $initialQid")
            viewModel.loadQuizByQid(initialQid)
        } else {
            Log.w(TAG, "Non-random mode and no initial QID. Loading first quiz (or handle error?).")
            // Decide default behavior: load random, load first, or show error?
            // For now, loading random as a fallback.
            viewModel.loadRandomQuizId() // Or show an error message
        }
        Log.d(TAG, "onViewCreated: Finished loading logic.")
        
        // 戻るボタンのカスタム処理を追加
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 解いた問題IDを取得
                val lastSolvedId = viewModel.lastSolvedQuizId.value
                
                // 問題一覧へのナビゲーション引数を設定
                val bundle = Bundle().apply {
                    putString("lastSolvedQuizId", lastSolvedId)
                }
                
                // 問題一覧画面に戻る
                findNavController().navigate(R.id.action_quizFragment_to_problemListFragment, bundle)
                
                // 必要に応じて、このコールバックを無効化することもできます
                // isEnabled = false
            }
        })
    }

    private fun setupObservers() {
        viewModel.currentQuiz.observe(viewLifecycleOwner) { quiz: QuizEntity? ->
            Log.d(TAG, "Observer received quiz update for menu invalidation: ${quiz?.qid}, Bookmarked: ${quiz?.isBookmarked}")
            menuHost.invalidateMenu()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error: String? ->
            if (error?.isNotEmpty() == true) {
                Log.e(TAG, "Error message observed: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean ->
            Log.d(TAG, "Loading state changed: $isLoading")
        }
    }

    private fun setupMenu() {
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.quiz_menu, menu)
                Log.d(TAG, "MenuProvider: onCreateMenu called")
    }

            override fun onPrepareMenu(menu: Menu) {
        val bookmarkItem = menu.findItem(R.id.action_bookmark)
                val currentQuiz = viewModel.currentQuiz.value
                Log.d(TAG, "MenuProvider: onPrepareMenu called, currentQuiz: ${currentQuiz?.qid}, bookmarked: ${currentQuiz?.isBookmarked}")

        bookmarkItem?.let { item ->
            if (currentQuiz != null) {
                item.isVisible = true
                        val isBookmarked = currentQuiz.isBookmarked
                        val iconDrawableId = if (isBookmarked) {
                            R.drawable.ic_bookmark_filled
                } else {
                            R.drawable.ic_bookmark_border_24dp
                }
                val rawDrawable = ContextCompat.getDrawable(requireContext(), iconDrawableId)
                if (rawDrawable != null) {
                    val wrappedDrawable = DrawableCompat.wrap(rawDrawable).mutate()
                    DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(requireContext(), android.R.color.white))
                    item.icon = wrappedDrawable
                }
                        item.title = if (isBookmarked) "ブックマーク解除" else "ブックマーク追加"
            } else {
                item.isVisible = false
            }
                } ?: Log.w(TAG, "MenuProvider: Bookmark menu item not found in onPrepareMenu")
    }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
            R.id.action_bookmark -> {
                viewModel.currentQuiz.value?.let { currentQuiz ->
                            Log.d(TAG, "MenuProvider: Bookmark menu item clicked for QID: ${currentQuiz.qid}")
                    viewModel.toggleBookmarkStatus(currentQuiz)
                    true
                } ?: run {
                            Log.w(TAG, "MenuProvider: Bookmark clicked but current quiz is null.")
                    Toast.makeText(requireContext(), "クイズを読み込めません", Toast.LENGTH_SHORT).show()
                    false
            }
        }
                    else -> false
                }
        }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
    }

    companion object {
        private const val TAG = "QuizFragment"
    }
}
