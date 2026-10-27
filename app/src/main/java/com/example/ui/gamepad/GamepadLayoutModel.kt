package com.example.ui.gamepad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class GamepadControlId(val displayName: String) {
    SHOULDER_LEFT("Left Triggers (L1/L2)"),
    SHOULDER_RIGHT("Right Triggers (R1/R2)"),
    MENU_BUTTONS("Menu (Select/Start)"),
    DPAD("Directional Pad (D-Pad)"),
    LEFT_STICK("Left Analog Stick"),
    RIGHT_STICK("Right Analog Stick"),
    ABXY("Action Diamond (A B X Y)"),
    TURBO("Turbo / Special Hotkey")
}

enum class GamepadPreset(val label: String, val description: String) {
    DEFAULT_CONSOLE("Modern Console", "Ergonomic layout with D-Pad & Left Stick on left, ABXY & Right Stick on right"),
    TWIN_STICK("Twin Stick Action", "Prominent dual thumbsticks with top triggers for shooters & 3D games"),
    RETRO_ARCADE("Retro Arcade", "Large D-Pad & ABXY buttons optimized for classic 2D platformers"),
    SOUTHPAW("Southpaw / Inverted", "Inverted layout for left-handed gamers (ABXY on left, sticks on right)")
}

data class ControlLayoutConfig(
    val id: GamepadControlId,
    val normX: Float, // Normalized X position (0.0 to 1.0)
    val normY: Float, // Normalized Y position (0.0 to 1.0)
    val scale: Float = 1.0f,
    val visible: Boolean = true
)

data class GamepadLayoutState(
    val controls: Map<GamepadControlId, ControlLayoutConfig>,
    val preset: GamepadPreset = GamepadPreset.DEFAULT_CONSOLE,
    val opacity: Float = 0.95f,
    val hapticsEnabled: Boolean = true,
    val turboIntervalMs: Long = 80L
)

object GamepadLayoutStorage {
    private const val PREFS_NAME = "omnidesk_gamepad_config"
    private const val KEY_PRESET = "selected_preset"
    private const val KEY_OPACITY = "control_opacity"
    private const val KEY_HAPTICS = "haptics_enabled"
    private const val KEY_CONTROLS_JSON = "custom_controls_json"

    fun getPresetDefaults(preset: GamepadPreset): Map<GamepadControlId, ControlLayoutConfig> {
        return when (preset) {
            GamepadPreset.DEFAULT_CONSOLE -> mapOf(
                GamepadControlId.SHOULDER_LEFT to ControlLayoutConfig(GamepadControlId.SHOULDER_LEFT, normX = 0.12f, normY = 0.15f, scale = 1.0f),
                GamepadControlId.SHOULDER_RIGHT to ControlLayoutConfig(GamepadControlId.SHOULDER_RIGHT, normX = 0.88f, normY = 0.15f, scale = 1.0f),
                GamepadControlId.MENU_BUTTONS to ControlLayoutConfig(GamepadControlId.MENU_BUTTONS, normX = 0.50f, normY = 0.16f, scale = 1.0f),
                GamepadControlId.DPAD to ControlLayoutConfig(GamepadControlId.DPAD, normX = 0.14f, normY = 0.56f, scale = 1.0f),
                GamepadControlId.LEFT_STICK to ControlLayoutConfig(GamepadControlId.LEFT_STICK, normX = 0.32f, normY = 0.76f, scale = 1.0f),
                GamepadControlId.RIGHT_STICK to ControlLayoutConfig(GamepadControlId.RIGHT_STICK, normX = 0.68f, normY = 0.76f, scale = 1.0f),
                GamepadControlId.ABXY to ControlLayoutConfig(GamepadControlId.ABXY, normX = 0.86f, normY = 0.56f, scale = 1.0f),
                GamepadControlId.TURBO to ControlLayoutConfig(GamepadControlId.TURBO, normX = 0.50f, normY = 0.84f, scale = 0.9f, visible = true)
            )

            GamepadPreset.TWIN_STICK -> mapOf(
                GamepadControlId.SHOULDER_LEFT to ControlLayoutConfig(GamepadControlId.SHOULDER_LEFT, normX = 0.12f, normY = 0.14f, scale = 1.05f),
                GamepadControlId.SHOULDER_RIGHT to ControlLayoutConfig(GamepadControlId.SHOULDER_RIGHT, normX = 0.88f, normY = 0.14f, scale = 1.05f),
                GamepadControlId.MENU_BUTTONS to ControlLayoutConfig(GamepadControlId.MENU_BUTTONS, normX = 0.50f, normY = 0.14f, scale = 0.95f),
                GamepadControlId.LEFT_STICK to ControlLayoutConfig(GamepadControlId.LEFT_STICK, normX = 0.15f, normY = 0.58f, scale = 1.25f),
                GamepadControlId.DPAD to ControlLayoutConfig(GamepadControlId.DPAD, normX = 0.33f, normY = 0.74f, scale = 0.9f),
                GamepadControlId.ABXY to ControlLayoutConfig(GamepadControlId.ABXY, normX = 0.85f, normY = 0.45f, scale = 1.05f),
                GamepadControlId.RIGHT_STICK to ControlLayoutConfig(GamepadControlId.RIGHT_STICK, normX = 0.70f, normY = 0.74f, scale = 1.25f),
                GamepadControlId.TURBO to ControlLayoutConfig(GamepadControlId.TURBO, normX = 0.50f, normY = 0.84f, scale = 0.9f, visible = true)
            )

            GamepadPreset.RETRO_ARCADE -> mapOf(
                GamepadControlId.SHOULDER_LEFT to ControlLayoutConfig(GamepadControlId.SHOULDER_LEFT, normX = 0.14f, normY = 0.15f, scale = 1.1f),
                GamepadControlId.SHOULDER_RIGHT to ControlLayoutConfig(GamepadControlId.SHOULDER_RIGHT, normX = 0.86f, normY = 0.15f, scale = 1.1f),
                GamepadControlId.MENU_BUTTONS to ControlLayoutConfig(GamepadControlId.MENU_BUTTONS, normX = 0.50f, normY = 0.20f, scale = 1.0f),
                GamepadControlId.DPAD to ControlLayoutConfig(GamepadControlId.DPAD, normX = 0.18f, normY = 0.60f, scale = 1.25f),
                GamepadControlId.ABXY to ControlLayoutConfig(GamepadControlId.ABXY, normX = 0.82f, normY = 0.60f, scale = 1.25f),
                GamepadControlId.LEFT_STICK to ControlLayoutConfig(GamepadControlId.LEFT_STICK, normX = 0.38f, normY = 0.78f, scale = 0.85f, visible = false),
                GamepadControlId.RIGHT_STICK to ControlLayoutConfig(GamepadControlId.RIGHT_STICK, normX = 0.62f, normY = 0.78f, scale = 0.85f, visible = false),
                GamepadControlId.TURBO to ControlLayoutConfig(GamepadControlId.TURBO, normX = 0.50f, normY = 0.75f, scale = 1.0f, visible = true)
            )

            GamepadPreset.SOUTHPAW -> mapOf(
                GamepadControlId.SHOULDER_LEFT to ControlLayoutConfig(GamepadControlId.SHOULDER_LEFT, normX = 0.12f, normY = 0.15f, scale = 1.0f),
                GamepadControlId.SHOULDER_RIGHT to ControlLayoutConfig(GamepadControlId.SHOULDER_RIGHT, normX = 0.88f, normY = 0.15f, scale = 1.0f),
                GamepadControlId.MENU_BUTTONS to ControlLayoutConfig(GamepadControlId.MENU_BUTTONS, normX = 0.50f, normY = 0.16f, scale = 1.0f),
                GamepadControlId.ABXY to ControlLayoutConfig(GamepadControlId.ABXY, normX = 0.14f, normY = 0.56f, scale = 1.0f),
                GamepadControlId.RIGHT_STICK to ControlLayoutConfig(GamepadControlId.RIGHT_STICK, normX = 0.32f, normY = 0.76f, scale = 1.0f),
                GamepadControlId.LEFT_STICK to ControlLayoutConfig(GamepadControlId.LEFT_STICK, normX = 0.68f, normY = 0.76f, scale = 1.0f),
                GamepadControlId.DPAD to ControlLayoutConfig(GamepadControlId.DPAD, normX = 0.86f, normY = 0.56f, scale = 1.0f),
                GamepadControlId.TURBO to ControlLayoutConfig(GamepadControlId.TURBO, normX = 0.50f, normY = 0.84f, scale = 0.9f, visible = true)
            )
        }
    }

    fun loadLayout(context: Context): GamepadLayoutState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetName = prefs.getString(KEY_PRESET, GamepadPreset.DEFAULT_CONSOLE.name)
        val preset = try {
            GamepadPreset.valueOf(presetName ?: GamepadPreset.DEFAULT_CONSOLE.name)
        } catch (_: Exception) {
            GamepadPreset.DEFAULT_CONSOLE
        }

        val opacity = prefs.getFloat(KEY_OPACITY, 0.95f)
        val haptics = prefs.getBoolean(KEY_HAPTICS, true)
        val jsonString = prefs.getString(KEY_CONTROLS_JSON, null)

        val defaultControls = getPresetDefaults(preset)
        if (jsonString.isNullOrEmpty()) {
            return GamepadLayoutState(
                controls = defaultControls,
                preset = preset,
                opacity = opacity,
                hapticsEnabled = haptics
            )
        }

        return try {
            val jsonArray = JSONArray(jsonString)
            val customMap = mutableMapOf<GamepadControlId, ControlLayoutConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val idStr = obj.optString("id")
                val controlId = try { GamepadControlId.valueOf(idStr) } catch (_: Exception) { null }
                if (controlId != null) {
                    val normX = obj.optDouble("normX", 0.5).toFloat()
                    val normY = obj.optDouble("normY", 0.5).toFloat()
                    val scale = obj.optDouble("scale", 1.0).toFloat()
                    val visible = obj.optBoolean("visible", true)
                    customMap[controlId] = ControlLayoutConfig(controlId, normX, normY, scale, visible)
                }
            }

            // Fill missing with defaults
            val merged = defaultControls.toMutableMap()
            merged.putAll(customMap)
            GamepadLayoutState(
                controls = merged,
                preset = preset,
                opacity = opacity,
                hapticsEnabled = haptics
            )
        } catch (_: Exception) {
            GamepadLayoutState(
                controls = defaultControls,
                preset = preset,
                opacity = opacity,
                hapticsEnabled = haptics
            )
        }
    }

    fun saveLayout(context: Context, state: GamepadLayoutState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        state.controls.values.forEach { config ->
            val obj = JSONObject().apply {
                put("id", config.id.name)
                put("normX", config.normX.toDouble())
                put("normY", config.normY.toDouble())
                put("scale", config.scale.toDouble())
                put("visible", config.visible)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_PRESET, state.preset.name)
            .putFloat(KEY_OPACITY, state.opacity)
            .putBoolean(KEY_HAPTICS, state.hapticsEnabled)
            .putString(KEY_CONTROLS_JSON, jsonArray.toString())
            .apply()
    }

    fun resetLayout(context: Context, preset: GamepadPreset): GamepadLayoutState {
        val defaults = getPresetDefaults(preset)
        val state = GamepadLayoutState(
            controls = defaults,
            preset = preset,
            opacity = 0.95f,
            hapticsEnabled = true
        )
        saveLayout(context, state)
        return state
    }
}
