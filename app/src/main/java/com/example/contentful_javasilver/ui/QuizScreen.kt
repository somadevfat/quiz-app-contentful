// app/src/main/java/com/example/contentful_javasilver/ui/QuizScreen.kt
package com.example.contentful_javasilver.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contentful_javasilver.data.QuizEntity
import com.example.contentful_javasilver.viewmodels.QuizViewModel
import androidx.compose.foundation.isSystemInDarkTheme // Import for theme check

@Composable
fun QuizScreen(
    quizViewModel: QuizViewModel,
    isRandomMode: Boolean // Receive isRandomMode flag
) {
    // Observe ViewModel LiveData using observeAsState
    val currentQuizFromState: QuizEntity? by quizViewModel.currentQuiz.observeAsState(initial = null)
    val isLoading: Boolean by quizViewModel.isLoading.observeAsState(initial = true) // Restore loading state observation
    val errorMessage: String? by quizViewModel.errorMessage.observeAsState(initial = null) // Restore error state observation

    // Observe ViewModel StateFlows using collectAsStateWithLifecycle
    val isAnswered: Boolean by quizViewModel.isAnswered.collectAsStateWithLifecycle()
    val userSelections: Set<Int> by quizViewModel.userSelections.collectAsStateWithLifecycle()
    val answerResult: QuizViewModel.AnswerResult? by quizViewModel.answerResult.collectAsStateWithLifecycle()

    // Define callbacks for actions
    val loadNext = { quizViewModel.loadNextQuiz(isRandomMode) }

    // Capture LiveData state value into a local stable variable for smart casting
    val currentQuiz = currentQuizFromState

    // --- Restore Original Code with when statement ---
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Center content like loading indicator
    ) {
        when {
            // Corrected: Only show loading indicator when isLoading is true
            isLoading -> {
                CircularProgressIndicator()
            }
            errorMessage != null -> {
                // Show error message
                Text(
                    text = "エラーが発生しました: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            // Ensure currentQuiz is not null before calling QuizContent
            currentQuiz != null -> {
                // Only show content when quiz is actually loaded
                QuizContent(
                    quiz = currentQuiz,
                    isAnswered = isAnswered,
                    userSelections = userSelections,
                    answerResult = answerResult,
                    onSubmitAnswer = { index -> quizViewModel.submitAnswer(index) },
                    onSubmitMultipleAnswer = { selections -> quizViewModel.submitAnswer(selections) },
                    onNextClicked = loadNext,
                    onSkipClicked = loadNext
                )
            }
            // Modified else block to show a fallback message
            else -> {
                 // isLoading=false, errorMessage=null, currentQuiz=null の場合
                 Text(
                     text = "問題データを取得できませんでした。再試行してください。", // More specific message
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Slightly dimmed color
                     modifier = Modifier.padding(16.dp)
                 )
                 Log.w("QuizScreen", "State displayed: Not loading, no error, but currentQuiz is null.") // Updated log
             }
        }
    }
    // --- END Restore Original Code ---

    /* // Temporarily removed loading/error handling code commented out
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center 
    ) {
        if (currentQuiz != null) {
             QuizContent(
                quiz = currentQuiz,
                isAnswered = isAnswered,
                userSelections = userSelections,
                answerResult = answerResult,
                onSubmitAnswer = { index -> quizViewModel.submitAnswer(index) },
                onSubmitMultipleAnswer = { selections -> quizViewModel.submitAnswer(selections) },
                onNextClicked = loadNext,
                onSkipClicked = loadNext
            )
        } else {
            // Display a simple message if quiz data is not yet available
            Text("クイズデータを準備中...", modifier = Modifier.padding(16.dp))
        }
    }
    */
}

@Composable
fun QuizContent(
    quiz: QuizEntity,
    isAnswered: Boolean, // Receive from ViewModel state
    userSelections: Set<Int>, // Receive from ViewModel state (used after answering)
    answerResult: QuizViewModel.AnswerResult?, // Receive from ViewModel state
    onSubmitAnswer: (Int) -> Unit, // Callback for single choice submission
    onSubmitMultipleAnswer: (Set<Int>) -> Unit, // Callback for multiple choice submission
    onNextClicked: () -> Unit, // Add callback for Next
    onSkipClicked: () -> Unit // Add callback for Skip
) {
    val isMultipleChoice = quiz.answer?.size ?: 0 > 1
    val scrollState = rememberScrollState() // Remember the scroll state

    // State for single choice selection *before* submitting
    val selectedSingleChoiceIndex = remember(quiz.qid) { mutableStateOf<Int?>(null) }
    // State for multiple choice selections *before* submitting (既存)
    val currentMultipleSelections = remember(quiz.qid) { mutableStateListOf<Int>() }

    // Reset selection states when quiz changes or when answer is submitted
    LaunchedEffect(quiz.qid, isAnswered) {
        if (!isAnswered) { // Only reset if moving to a new question (not answered yet)
           selectedSingleChoiceIndex.value = null
           currentMultipleSelections.clear()
           // Also scroll to top when loading a new question
           scrollState.scrollTo(0)
        }
    }

    // Scroll to the explanation card when the answer is submitted
    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            // Animate scroll to the bottom (where explanation appears)
            // Ensure the scroll happens after the content size has potentially changed
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Use the remembered scroll state
            .navigationBarsPadding() // <<<--- Add this modifier
            .padding(16.dp) // Add padding around the content
    ) {
        // Question Area in a Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp), // Space below the card
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Subtle elevation
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // QID Text (Optional, could be integrated elsewhere)
                Text(
                    text = "問題 ${quiz.qid}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Question Text
                Text(
                    text = quiz.questionText ?: "質問を読み込めません。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Code Block Area in a Card (If code exists)
        if (!quiz.code.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                 elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Less elevation than question
                 colors = CardDefaults.cardColors(
                     containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceVariant
                 )
            ) {
                // Use horizontal scroll if code is long? For now, just wrap.
                Text(
                    text = quiz.code,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace, // Use monospace font
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add space before choices

        // Choices Area
        QuizChoices(
            choices = quiz.choices ?: emptyList(),
            isMultipleChoice = isMultipleChoice,
            isAnswered = isAnswered,
            // Pass the correct selection state based on whether it's answered or not
            currentSelections = if (isAnswered) userSelections else if (isMultipleChoice) currentMultipleSelections.toSet() else selectedSingleChoiceIndex.value?.let { setOf(it) } ?: emptySet(),
            answerResult = answerResult,
            // Callback to update the local selection state
            onSelectionChange = { index, isSelected ->
                if (!isAnswered) { // Only allow changes if not answered
                    if (isMultipleChoice) {
                        if (isSelected) {
                            currentMultipleSelections.add(index)
                        } else {
                            currentMultipleSelections.remove(index)
                        }
                    } else {
                        // For single choice, selecting one deselects others (handled by setting the state)
                        selectedSingleChoiceIndex.value = index
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // "回答する" Button (Always visible when not answered)
        if (!isAnswered) {
            val isSubmitEnabled = if (isMultipleChoice) {
                currentMultipleSelections.isNotEmpty()
            } else {
                selectedSingleChoiceIndex.value != null
            }
            ElevatedButton(
                onClick = {
                    if (isMultipleChoice) {
                        onSubmitMultipleAnswer(currentMultipleSelections.toSet())
                    } else {
                        // Submit the single selected index
                        selectedSingleChoiceIndex.value?.let { onSubmitAnswer(it) }
                    }
                },
                enabled = isSubmitEnabled, // Enable based on selection
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Keep consistent height
                shape = MaterialTheme.shapes.medium, // Keep consistent shape
                colors = ButtonDefaults.elevatedButtonColors( // Keep consistent colors
                    containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                ),
                elevation = ButtonDefaults.elevatedButtonElevation( // Keep consistent elevation
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Row( // Keep consistent layout
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "回答する",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Keep spacing
        }

        // Explanation display (conditional) - Use the extracted Composable
        AnimatedVisibility(visible = isAnswered) {
            QuizResultCard(quiz = quiz, answerResult = answerResult)
        }

        // Bottom Action Buttons (conditional)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = if (isAnswered) Arrangement.End else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isAnswered) {
                // Show Skip button only when not answered (改良版)
                OutlinedButton(
                    onClick = onSkipClicked,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("スキップ")
                    }
                }
            }

            if (isAnswered) {
                // Show Next button only when answered (改良版)
                ElevatedButton(
                    onClick = onNextClicked,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "次へ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- Extracted Composable for Result/Explanation ---
@Composable
fun QuizResultCard(
    quiz: QuizEntity,
    answerResult: QuizViewModel.AnswerResult?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             // 定義済みの色を再利用
             val correctColor = Color(0xFF4CAF50) // Standard Green
             val incorrectColor = MaterialTheme.colorScheme.error // Use theme's error color for Red
            
             // --- Result and Selections Row ---
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(bottom = 12.dp), // Space below this row
                 verticalAlignment = Alignment.Top // Align items to the top
             ) {
                 // "正解！" or "不正解" Text
                 Text(
                     text = if (answerResult?.isCorrect == true) "正解！" else "不正解",
                     style = MaterialTheme.typography.titleMedium,
                     color = if (answerResult?.isCorrect == true) correctColor else MaterialTheme.colorScheme.error,
                     modifier = Modifier.padding(end = 16.dp) // Space between result and selections
                 )
                 
                 // --- Selections Column ---
                 Column {
                     // 正解の選択肢
                     if (answerResult?.correctAnswers?.isNotEmpty() == true) {
                         Text(
                             text = "正解: " + answerResult.correctAnswers.sorted().joinToString(", ") { 
                                 val index = it
                                 val letter = ('A' + index).toChar()
                                 "$letter" // 選択肢の内容を表示しない
                             },
                             style = MaterialTheme.typography.bodyMedium,
                             color = correctColor, // 緑色
                             modifier = Modifier.padding(bottom = 4.dp)
                         )
                     }
                     
                     // ユーザーの選択
                     if (answerResult?.userSelections?.isNotEmpty() == true) {
                         val userSelectionsText = answerResult.userSelections.sorted().joinToString(", ") { 
                             val index = it
                             val letter = ('A' + index).toChar()
                             "$letter" // 選択肢の内容を表示しない
                         }
                         val isCorrect = answerResult.isCorrect
                         Text(
                             text = "あなたの選択: $userSelectionsText",
                             style = MaterialTheme.typography.bodyMedium,
                             color = if (isCorrect) correctColor else incorrectColor, // 正解なら緑、不正解なら赤
                             //modifier = Modifier.padding(bottom = 8.dp) // Removed bottom padding here
                         )
                     }
                 }
             }
             
             // --- Divider ---
             Divider(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 8.dp),
                 color = MaterialTheme.colorScheme.outlineVariant
             )
            
            // 解説文ヘッダー
            Text(
                text = "解説:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
                // Apply theme-appropriate color to header
                color = MaterialTheme.colorScheme.onSurface 
            )
            // 解説文本文
            Text(
                text = quiz.explanation ?: "解説はありません。",
                style = MaterialTheme.typography.bodyMedium,
                // Apply white color in dark mode, default otherwise
                color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// --- End of Extracted Composable ---

@Composable
fun QuizChoices(
    choices: List<String>,
    isMultipleChoice: Boolean,
    isAnswered: Boolean,
    currentSelections: Set<Int>, // Combined state for current selections (before or after answer)
    answerResult: QuizViewModel.AnswerResult?, // From ViewModel
    onSelectionChange: (Int, Boolean) -> Unit // Unified callback for selection changes
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        choices.forEachIndexed { index, choiceText ->
            // Determine the visual state based on answered status and results
            val selectionState: SelectionState = when {
                !isAnswered -> if (currentSelections.contains(index)) SelectionState.SELECTED_UNANSWERED else SelectionState.UNANSWERED
                answerResult == null -> SelectionState.DEFAULT // Should not happen if answered, but default case
                answerResult.correctAnswers.contains(index) && currentSelections.contains(index) -> SelectionState.CORRECT_SELECTED
                answerResult.correctAnswers.contains(index) && !currentSelections.contains(index) -> SelectionState.CORRECT_NOT_SELECTED
                !answerResult.correctAnswers.contains(index) && currentSelections.contains(index) -> SelectionState.INCORRECT_SELECTED
                else -> SelectionState.DEFAULT // Other choices after answer
            }
            val isCurrentlySelected = currentSelections.contains(index)

            ChoiceRow(
                text = choiceText,
                isSelected = isCurrentlySelected, // Pass current selection status for UI state
                selectionState = selectionState, // Pass detailed state for styling
                isMultipleChoice = isMultipleChoice,
                enabled = !isAnswered, // Choices are only enabled before answering
                onClick = {
                    // Call the unified callback, passing the index and the *new* selected state
                    onSelectionChange(index, !isCurrentlySelected)
                }
            )
            Spacer(modifier = Modifier.height(8.dp)) // Spacing between choices
        }
    }
}

// Updated enum for more granular state
enum class SelectionState {
    UNANSWERED,             // Not selected, not answered
    SELECTED_UNANSWERED,    // Selected, not answered
    CORRECT_SELECTED,       // Was correct, was selected by user
    CORRECT_NOT_SELECTED,   // Was correct, but not selected by user (missed)
    INCORRECT_SELECTED,     // Was incorrect, but selected by user
    DEFAULT                 // Default state after answer (incorrect, not selected)
}

@Composable
fun ChoiceRow(
    text: String,
    isSelected: Boolean, // Is this specific row currently considered "selected" (visually)?
    selectionState: SelectionState, // Detailed state for styling
    isMultipleChoice: Boolean,
    enabled: Boolean, // Is interaction allowed?
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium // Consistent shape

    // --- Define Colors based on state ---
    val correctColor = Color(0xFF4CAF50) // Consistent Green
    val incorrectColor = MaterialTheme.colorScheme.error // Consistent Red from theme

    // --- Modified Colors based on User Request (with subtle border) ---
    // Background: Always transparent
    val backgroundColor = Color.Transparent

    // Border: Subtle border based on state
    val targetBorderColor = when (selectionState) {
        SelectionState.UNANSWERED -> MaterialTheme.colorScheme.outlineVariant // Subtle border for unanswered
        SelectionState.SELECTED_UNANSWERED -> MaterialTheme.colorScheme.primary // Primary color when selected
        SelectionState.CORRECT_SELECTED -> correctColor
        SelectionState.CORRECT_NOT_SELECTED -> correctColor // Highlight border even if not selected
        SelectionState.INCORRECT_SELECTED -> incorrectColor
        SelectionState.DEFAULT -> MaterialTheme.colorScheme.outlineVariant // Subtle border for others after answer
    }
    val borderColor by animateColorAsState(targetBorderColor, animationSpec = tween(300))

    // Content (Text): Always White
    val targetContentColor = Color.White

    // --- Icon Determination (Keep existing logic) ---
    val iconVector = when (selectionState) {
        SelectionState.SELECTED_UNANSWERED,
        SelectionState.CORRECT_SELECTED,
        SelectionState.INCORRECT_SELECTED -> if (isMultipleChoice) Icons.Filled.CheckBox else Icons.Filled.RadioButtonChecked
        SelectionState.UNANSWERED,
        SelectionState.CORRECT_NOT_SELECTED,
        SelectionState.DEFAULT -> if (isMultipleChoice) Icons.Filled.CheckBoxOutlineBlank else Icons.Filled.RadioButtonUnchecked
    }
    val iconContentDescription = if (isSelected) "Selected" else "Not Selected"
    // Adjust Icon Tint (Keep previous adjustment)
    val iconTintColor = when (selectionState) {
         SelectionState.CORRECT_SELECTED,
         SelectionState.CORRECT_NOT_SELECTED -> correctColor // Keep Green for correct
         SelectionState.INCORRECT_SELECTED -> incorrectColor // Keep Red for incorrect
         SelectionState.SELECTED_UNANSWERED -> MaterialTheme.colorScheme.primary // Theme primary for selected but unanswered
         else -> Color.White // Default icon color (Unanswered/Default)
    }

    // --- Row Layout ---
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Re-add the border modifier with dynamic color
            .border(BorderStroke(1.dp, borderColor), shape = shape)
            // Set background to transparent
            .background(backgroundColor, shape = shape) // backgroundColor is still Color.Transparent
            .clip(shape) // Apply clipping *after* background and border
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true) // Use ripple effect
            )
            .padding(horizontal = 16.dp, vertical = 12.dp), // Adjust padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start // Align items to the start
    ) {
        // --- Icon --- (Keep icon, tint is adjusted)
        Icon(
            imageVector = iconVector,
            contentDescription = iconContentDescription,
            tint = iconTintColor,
            modifier = Modifier.size(24.dp) // Standard icon size
        )

        Spacer(modifier = Modifier.width(12.dp)) // Space between icon and text

        // --- Text --- (Keep color white)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = targetContentColor,
            modifier = Modifier.weight(1f) // Allow text to take remaining space
        )

         // --- Result Indicator (Keep existing logic) ---
         AnimatedVisibility(
             visible = selectionState == SelectionState.CORRECT_SELECTED ||
                     selectionState == SelectionState.CORRECT_NOT_SELECTED ||
                     selectionState == SelectionState.INCORRECT_SELECTED,
             enter = fadeIn(),
             exit = fadeOut()
         ) {
             val resultIcon = when (selectionState) {
                 SelectionState.CORRECT_SELECTED, SelectionState.CORRECT_NOT_SELECTED -> Icons.Filled.CheckCircle // Green check for correct
                 SelectionState.INCORRECT_SELECTED -> Icons.Filled.Cancel // Red cross for incorrect selection
                 else -> null // Should not happen in visible state
             }
             val resultIconColor = when (selectionState) {
                 SelectionState.CORRECT_SELECTED, SelectionState.CORRECT_NOT_SELECTED -> correctColor
                 SelectionState.INCORRECT_SELECTED -> incorrectColor
                 else -> Color.Unspecified
             }

             if (resultIcon != null) {
                 Spacer(modifier = Modifier.width(8.dp)) // Space before result icon
                 Icon(
                     imageVector = resultIcon,
                     contentDescription = if (selectionState == SelectionState.CORRECT_SELECTED || selectionState == SelectionState.CORRECT_NOT_SELECTED) "Correct" else "Incorrect",
                     tint = resultIconColor,
                     modifier = Modifier.size(20.dp) // Slightly smaller result icon
                 )
             }
         }
    }
}

// Define ContentAlpha if not available (add near top-level)
object ContentAlpha {
    val high: Float = 1.0f
    val medium: Float = 0.74f
    val disabled: Float = 0.38f
} 