package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ApiKeyStatus {
    object Valid : ApiKeyStatus
    object Missing : ApiKeyStatus
    data class Error(val message: String) : ApiKeyStatus
}

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    // List of all user chat sessions
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected session ID
    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    // Current active persona identifier
    private val _selectedPersonaId = MutableStateFlow(Personas.GENERAL.id)
    val selectedPersonaId: StateFlow<String> = _selectedPersonaId.asStateFlow()

    // Stream of messages for the currently selected session
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForSession(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Valid)
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    // Real-Time Global MoneyFlow Data sources
    private val _cryptoAssets = MutableStateFlow<List<CoinData>>(emptyList())
    val cryptoAssets: StateFlow<List<CoinData>> = _cryptoAssets.asStateFlow()

    private val _forexExchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val forexExchangeRates: StateFlow<Map<String, Double>> = _forexExchangeRates.asStateFlow()

    private val _isRefreshingFinancialData = MutableStateFlow(false)
    val isRefreshingFinancialData: StateFlow<Boolean> = _isRefreshingFinancialData.asStateFlow()

    private val _financialDataLastUpdated = MutableStateFlow("")
    val financialDataLastUpdated: StateFlow<String> = _financialDataLastUpdated.asStateFlow()

    init {
        checkApiKey()
        fetchRealFinancialData()
        // Auto-initialize a session if there are none when loaded
        viewModelScope.launch {
            repository.allSessions.first().let { list ->
                if (list.isEmpty()) {
                    createNewSession(Personas.GENERAL.id)
                } else {
                    selectSession(list.first().id)
                }
            }
        }
    }

    fun fetchRealFinancialData() {
        viewModelScope.launch {
            _isRefreshingFinancialData.value = true
            try {
                // Fetch crypto assets from CoinCap (limit to 6 for clean layout)
                val cryptoResponse = RealDataClient.coinCapService.getAssets(6)
                _cryptoAssets.value = cryptoResponse.data

                // Fetch forex rates relative to USD
                val forexResponse = RealDataClient.exchangeRateService.getLatestRates()
                if (forexResponse.result == "success") {
                    _forexExchangeRates.value = forexResponse.rates
                }

                val formatter = java.text.SimpleDateFormat("HH:mm:ss UTC", java.util.Locale.getDefault())
                _financialDataLastUpdated.value = formatter.format(java.util.Date())
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error fetching real financial data: ${e.message}", e)
            } finally {
                _isRefreshingFinancialData.value = false
            }
        }
    }

    private fun checkApiKey() {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY") {
            _apiKeyStatus.value = ApiKeyStatus.Missing
        } else {
            _apiKeyStatus.value = ApiKeyStatus.Valid
        }
    }

    fun selectSession(sessionId: Int) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _selectedPersonaId.value = session.personaId
            }
        }
        _errorMessage.value = null
    }

    fun createNewSession(personaId: String) {
        viewModelScope.launch {
            val persona = Personas.getById(personaId)
            val sessionTitle = "Chat with ${persona.name}"
            val newId = repository.createSession(sessionTitle, personaId)
            _currentSessionId.value = newId.toInt()
            _selectedPersonaId.value = personaId
            _errorMessage.value = null
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            // If deleted current session, clear selection to trigger auto-recreation/select
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
                val remaining = repository.allSessions.first()
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    val persona = Personas.getById(Personas.GENERAL.id)
                    val sessionTitle = "Chat with ${persona.name}"
                    val newId = repository.createSession(sessionTitle, Personas.GENERAL.id)
                    _currentSessionId.value = newId.toInt()
                    _selectedPersonaId.value = Personas.GENERAL.id
                    _errorMessage.value = null
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _currentSessionId.value = null
            
            val persona = Personas.getById(Personas.GENERAL.id)
            val sessionTitle = "Chat with ${persona.name}"
            val newId = repository.createSession(sessionTitle, Personas.GENERAL.id)
            _currentSessionId.value = newId.toInt()
            _selectedPersonaId.value = Personas.GENERAL.id
            _errorMessage.value = null
        }
    }

    fun renameSession(session: ChatSession, newTitle: String) {
        viewModelScope.launch {
            repository.updateSession(session.copy(title = newTitle))
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _currentSessionId.value ?: return
        if (text.trim().isBlank()) return

        _errorMessage.value = null
        _isGenerating.value = true

        viewModelScope.launch {
            // 1. Save user's message
            repository.saveMessage(sessionId, "USER", text)

            // 2. Load complete chat history for parent conversation memory
            val history = currentMessages.value

            // 3. Request generation from Gemini API
            val persona = Personas.getById(_selectedPersonaId.value)
            sendToGemini(sessionId, text, history, persona)
        }
    }

    private suspend fun sendToGemini(
        sessionId: Int,
        latestUserPrompt: String,
        history: List<ChatMessage>,
        persona: Persona
    ) = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Check key state
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            withContext(Dispatchers.Main) {
                _apiKeyStatus.value = ApiKeyStatus.Missing
                _isGenerating.value = false
                _errorMessage.value = "Gemini API Key is missing. Please configure it in the Secrets panel."
            }
            return@withContext
        }

        // Map database history into API Contents
        val apiContents = mutableListOf<Content>()
        
        // Add existing conversation history to keep context memory
        history.forEach { msg ->
            val role = if (msg.role == "USER") "user" else "model"
            apiContents.add(
                Content(
                    parts = listOf(Part(text = msg.text)),
                    role = role
                )
            )
        }

        // Add the current user prompt if it wasn't saved in the snapshot history
        if (apiContents.none { it.parts.firstOrNull()?.text == latestUserPrompt && it.role == "user" }) {
            apiContents.add(
                Content(
                    parts = listOf(Part(text = latestUserPrompt)),
                    role = "user"
                )
            )
        }

        // Setup system instructions based on the selected Persona
        val systemInstruction = Content(
            parts = listOf(Part(text = persona.systemInstruction))
        )

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val textOutput = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textOutput != null) {
                repository.saveMessage(sessionId, "MODEL", textOutput)
            } else {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "The AI returned an empty response. Let's try rephrasing."
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Connection error: ${e.localizedMessage ?: "Unknown server issue"}"
            }
        } finally {
            withContext(Dispatchers.Main) {
                _isGenerating.value = false
            }
        }
    }
}

class ChatViewModelFactory(
    private val application: Application,
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
