package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.NORMAL_SCORE_INCREASE
import com.example.unscramble.data.HELP_SCORE_INCREASE
import com.example.unscramble.data.CURRENT_SCORE_INCREASE
import com.example.unscramble.data.TWO_CORRECT_SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private lateinit var currentWord: String
    private lateinit var currentWordNotScrambled: String
    private var usedWords: MutableSet<String> = mutableSetOf()

    var userGuess by mutableStateOf("")
        private set

    init {
        resetGame()
    }

    fun receiveHelp() {
        CURRENT_SCORE_INCREASE = HELP_SCORE_INCREASE
        val stringHalfLength = currentWordNotScrambled.length / 2
        val firstHalf = currentWordNotScrambled.substring(0, stringHalfLength)
        updateUserGuess(firstHalf)
    }

    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    fun updateCorrectAnswersInARow() {
        _uiState.update { currentState ->
            currentState.copy(
                twoCorrectAnswersInARow = false
            )
        }
    }

    fun checkUserGuess() {
        var extraScore: Int = 0

        if (userGuess.equals(currentWord, ignoreCase = true)) {
            _uiState.update { currentState ->
                currentState.copy(
                    correctAnswers = currentState.correctAnswers.inc()
                )
            }
            if (_uiState.value.correctAnswers > 0 &&
                _uiState.value.correctAnswers % 2 == 0) {
                extraScore = TWO_CORRECT_SCORE_INCREASE
                _uiState.update { currentState ->
                    currentState.copy(
                        twoCorrectAnswersInARow = true
                    )
                }
            }
            val updatedScore = _uiState.value.score.plus(CURRENT_SCORE_INCREASE + extraScore)
            updateGameState(updatedScore)
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = true,
                    correctAnswers = 0
                )
            }
        }
        updateUserGuess("")
        CURRENT_SCORE_INCREASE = NORMAL_SCORE_INCREASE
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentWordCount = currentState.currentWordCount.inc(),
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    score = updatedScore
                )
            }
        }
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }
    private fun pickRandomWordAndShuffle(): String {
        // Continue picking up a new random word until you get one that hasn't been used before
        currentWord = allWords.random()
        currentWordNotScrambled = currentWord
        if (usedWords.contains(currentWord)) {
            return pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            return shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        // Scramble the word
        tempWord.shuffle()
        while (String(tempWord).equals(word)) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }
}