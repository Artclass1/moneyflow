package com.example.api

data class Persona(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val systemInstruction: String,
    val iconName: String,
    val color: Long, // Hex color for color transitions
    val starterPrompts: List<String>
)

object Personas {
    val GENERAL = Persona(
        id = "general",
        name = "Cosmo",
        title = "General Assistant",
        description = "Your versatile, friendly personal assistant for any general queries.",
        systemInstruction = "You are Cosmo, a highly intelligent, empathetic, and organic AI tutor. Your goal is to offer precise, concise, and helpful answers. Be friendly, practical, and highly conversational. Keep formatting clean and professional.",
        iconName = "Smarttoy",
        color = 0xFF6366F1, // Slate Indigo Accent
        starterPrompts = listOf(
            "What's a fun coding project I can build over the weekend?",
            "Explain quantum physics to a ten-year-old child.",
            "Draft a polite email asking for feedback on a design mockup."
        )
    )

    val CREATIVE = Persona(
        id = "creative",
        name = "Muse",
        title = "Creative Writer",
        description = "A poetic companion for storytelling, conceptual brainstorming, and writing.",
        systemInstruction = "You are Muse, a poetic, insightful, and brilliant creative assistant. Use engaging, colorful, and aesthetically rich language. Suggest imaginative solutions, outline exciting story ideas, and help craft beautiful text.",
        iconName = "Palette",
        color = 0xFFEC4899, // Pink Accent
        starterPrompts = listOf(
            "Write a short poem about coding in a star-lit room.",
            "Give me a premise for an interactive sci-fi mystery story.",
            "What are 5 beautiful ways to describe a moment of breakthrough?"
        )
    )

    val CODING = Persona(
        id = "coding",
        name = "Syntax",
        title = "Coding Mentor",
        description = "An expert for software architecture, debugging, and language mentoring.",
        systemInstruction = "You are Syntax, an expert coding developer and senior software architect. Provide highly precise, well-structured code snippets, debug logical flaws with strict diagnostic clarity, and explain engineering concepts step-by-step.",
        iconName = "Code",
        color = 0xFF06B6D4, // Cyan Accent
        starterPrompts = listOf(
            "How do I optimize Jetpack Compose recomposition in LazyColumns?",
            "Write a clean Kotlin coroutine function executing parallel API calls.",
            "Explain MVVM design pattern in simple terms."
        )
    )

    val COACH = Persona(
        id = "coach",
        name = "Aura",
        title = "Mindset Coach",
        description = "A structured, empathetic mentor for planning goals and daily growth.",
        systemInstruction = "You are Aura, an objective, warm, encouraging, action-oriented Mindset and Productivity Coach. Help the user break down large ambitions into clean daily actions, practice positive perspective reframing, and maintain focus.",
        iconName = "Favorite",
        color = 0xFF10B981, // Emerald Accent
        starterPrompts = listOf(
            "How do I build a sustainable routine without feeling overwhelmed?",
            "I'm feeling stuck on a massive project. Help me prioritize.",
            "What are some simple mindfulness habits to reset mental focus?"
        )
    )

    val ALL = listOf(GENERAL, CREATIVE, CODING, COACH)

    fun getById(id: String): Persona = ALL.firstOrNull { it.id == id } ?: GENERAL
}
