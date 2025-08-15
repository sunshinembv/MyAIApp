package com.example.myaiapp.chat.data.agents.summary_verifier_agent.model

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.Verify

data class Verification(val dto: Verify, val rawAssistant: OllamaChatMessage)