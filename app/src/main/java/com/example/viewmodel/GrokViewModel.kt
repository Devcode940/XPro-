package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "GrokViewModel"
private const val GROK_MODEL_NAME = "gemini-3.8-flash"
private const val GROK_TIMEOUT_MS = 60_000L

/**
 * Hard caps so a single request cannot grow without bound. There is no server-side rate limit or
 * quota guard yet, and prompt tokens are billed to the Firebase project.
 */
private const val MAX_PROMPT_CHARS = 8_000
private const val MAX_HISTORY_MESSAGES = 12
private const val MAX_HISTORY_CHARS_PER_MESSAGE = 4_000
private const val MAX_FEED_CONTEXT_POSTS = 5
private const val MAX_FEED_CONTEXT_CHARS = 4_000

enum class GrokMode(val label: String, val subtitle: String) {
    REGULAR("Regular", "Sharp, direct X-powered answers"),
    FUN("Fun Mode", "Witty, unfiltered X banter + facts")
}

enum class GrokRole {
    USER,
    MODEL
}

data class GrokMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: GrokRole,
    val text: String,
    val mode: GrokMode = GrokMode.REGULAR,
    val isStreaming: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class GrokUiState(
    val messages: List<GrokMessage> = emptyList(),
    val mode: GrokMode = GrokMode.REGULAR,
    val attachFeedContext: Boolean = true,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null
)

class GrokViewModel(application: Application) : AndroidViewModel(application) {

    private val repository by lazy { XProRepository.getInstance(application) }

    private val regularModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GROK_MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are Grok, the inbuilt AI assistant powered by X inside the XPro Android client. " +
                        "Provide crisp, accurate, insightful answers grounded in any provided X timeline and trend context. " +
                        "Avoid filler disclaimers and structure key takeaways clearly."
                )
            }
        )
    }

    private val funModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GROK_MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are Grok in Fun Mode, the inbuilt AI assistant powered by X inside the XPro Android client. " +
                        "Respond with sharp wit, playful banter, and zero corporate stiffness while remaining genuinely helpful and accurate."
                )
            }
        )
    }

    private val _uiState = MutableStateFlow(GrokUiState())
    val uiState: StateFlow<GrokUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun setMode(mode: GrokMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun toggleFeedContext(enabled: Boolean) {
        _uiState.update { it.copy(attachFeedContext = enabled) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, lastFailedPrompt = null) }
    }

    fun clearConversation() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isGenerating = false,
                errorMessage = null,
                lastFailedPrompt = null
            )
        }
    }

    fun retryLastMessage() {
        val prompt = _uiState.value.lastFailedPrompt ?: return
        _uiState.update { it.copy(errorMessage = null, lastFailedPrompt = null) }
        sendMessage(prompt, appendUserBubble = false)
    }

    fun sendMessage(rawPrompt: String, appendUserBubble: Boolean = true) {
        // Truncate before it reaches the model: the text field has no hard length limit and the
        // whole prompt is billed.
        val prompt = rawPrompt.trim().take(MAX_PROMPT_CHARS)
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        val currentMode = _uiState.value.mode
        val includeFeed = _uiState.value.attachFeedContext
        val historyMessages = _uiState.value.messages.filter { !it.isStreaming && it.text.isNotBlank() }

        val userMessage = if (appendUserBubble) {
            GrokMessage(role = GrokRole.USER, text = prompt, mode = currentMode)
        } else null
        val assistantMessageId = UUID.randomUUID().toString()
        val placeholderAssistant = GrokMessage(
            id = assistantMessageId,
            role = GrokRole.MODEL,
            text = "",
            mode = currentMode,
            isStreaming = true
        )

        _uiState.update { state ->
            val updated = buildList {
                addAll(state.messages)
                if (userMessage != null) add(userMessage)
                add(placeholderAssistant)
            }
            state.copy(
                messages = updated,
                isGenerating = true,
                errorMessage = null,
                lastFailedPrompt = null
            )
        }

        activeJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val contextSnapshot = if (includeFeed) buildXFeedContext() else ""
                val enrichedPrompt = if (contextSnapshot.isNotBlank()) {
                    "$contextSnapshot\n\nUser request:\n$prompt"
                } else {
                    prompt
                }

                val historyContent: List<Content> = historyMessages
                    .takeLast(MAX_HISTORY_MESSAGES)
                    .map { msg ->
                        content(role = if (msg.role == GrokRole.USER) "user" else "model") {
                            text(msg.text.take(MAX_HISTORY_CHARS_PER_MESSAGE))
                        }
                    }

                val model = when (currentMode) {
                    GrokMode.REGULAR -> regularModel
                    GrokMode.FUN -> funModel
                }

                withTimeout(GROK_TIMEOUT_MS) {
                    val chat = model.startChat(history = historyContent)
                    chat.sendMessageStream(enrichedPrompt).collect { chunk ->
                        val delta = chunk.text.orEmpty()
                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            val snapshotText = accumulated.toString()
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(text = snapshotText, isStreaming = true)
                                        } else {
                                            msg
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                val finalResponse = accumulated.toString().trim()
                if (finalResponse.isBlank()) {
                    removePlaceholderAndSetError(
                        assistantMessageId = assistantMessageId,
                        error = "Grok returned an empty response. Please try again.",
                        failedPrompt = prompt
                    )
                } else {
                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(text = finalResponse, isStreaming = false)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Grok streaming request timed out after ${GROK_TIMEOUT_MS}ms", e)
                finalizeOrRemoveWithError(
                    assistantMessageId = assistantMessageId,
                    partial = accumulated.toString(),
                    error = "Grok request timed out after 60 seconds. Please retry.",
                    failedPrompt = prompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Grok Firebase AI request failed", e)
                finalizeOrRemoveWithError(
                    assistantMessageId = assistantMessageId,
                    partial = accumulated.toString(),
                    error = e.localizedMessage ?: "Grok could not complete this request.",
                    failedPrompt = prompt
                )
            }
        }
    }

    private fun finalizeOrRemoveWithError(
        assistantMessageId: String,
        partial: String,
        error: String,
        failedPrompt: String
    ) {
        if (partial.isNotBlank()) {
            _uiState.update { state ->
                state.copy(
                    isGenerating = false,
                    errorMessage = error,
                    lastFailedPrompt = failedPrompt,
                    messages = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(text = partial, isStreaming = false)
                        } else {
                            msg
                        }
                    }
                )
            }
        } else {
            removePlaceholderAndSetError(assistantMessageId, error, failedPrompt)
        }
    }

    private fun removePlaceholderAndSetError(
        assistantMessageId: String,
        error: String,
        failedPrompt: String
    ) {
        _uiState.update { state ->
            state.copy(
                isGenerating = false,
                errorMessage = error,
                lastFailedPrompt = failedPrompt,
                messages = state.messages.filterNot { it.id == assistantMessageId }
            )
        }
    }

    private suspend fun buildXFeedContext(): String {
        return try {
            repository.ensureSeeded()
            val trends = repository.trends.first().filterNot { it.isPromoted }.take(4)
            val posts = repository.allPosts.first()
                .filterNot { it.isSponsored }
                .take(MAX_FEED_CONTEXT_POSTS)
            buildString {
                if (trends.isNotEmpty()) {
                    appendLine("Current XPro Trends:")
                    trends.forEach { t ->
                        appendLine("- ${t.topic} (${t.category}, ${t.postCountLabel})")
                    }
                }
                if (posts.isNotEmpty()) {
                    appendLine("Recent XPro Timeline Posts:")
                    posts.forEach { p ->
                        val titlePrefix = p.articleTitle?.let { "$it: " }.orEmpty()
                        appendLine("- ${p.authorName} (${p.authorHandle}): $titlePrefix${p.content.take(180)}")
                    }
                }
            }.trim().take(MAX_FEED_CONTEXT_CHARS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to load optional X feed context for Grok", e)
            ""
        }
    }
}
