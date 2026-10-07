package org.futo.inputmethod.latin.spellcheck

import android.content.Context
import android.graphics.Rect
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import org.futo.inputmethod.keyboard.Keyboard
import org.futo.inputmethod.keyboard.internal.KeyboardLayoutElement
import org.futo.inputmethod.keyboard.internal.KeyboardLayoutKind
import org.futo.inputmethod.keyboard.internal.KeyboardLayoutPage
import org.futo.inputmethod.latin.Subtypes
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2Params
import org.futo.inputmethod.v2keyboard.RegularKeyboardSize
import java.util.Locale

/**
 * Builds a dummy alphabet keyboard for a locale. The spell checker needs it for key
 * coordinates and proximity info, which the suggestion engine uses to rank corrections.
 */
object SpellCheckerKeyboardFactory {
    private const val TAG = "SpellCheckerKeyboard"

    // Same dummy size the old LatinIME spell checker used.
    private const val DUMMY_WIDTH = 480
    private const val DUMMY_HEIGHT = 301

    private const val FALLBACK_LAYOUT = "qwerty"

    @JvmStatic
    fun create(context: Context, locale: Locale): Keyboard? {
        return try {
            val layoutName = Subtypes.findClosestLocaleLayouts(context, locale).firstOrNull()
                ?: FALLBACK_LAYOUT

            val editorInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }

            val layoutSet = KeyboardLayoutSetV2(
                context,
                KeyboardLayoutSetV2Params(
                    computedSize = RegularKeyboardSize(
                        width = DUMMY_WIDTH,
                        height = DUMMY_HEIGHT,
                        padding = Rect()
                    ),
                    keyboardLayoutSet = layoutName,
                    locale = locale,
                    editorInfo = editorInfo,
                    numberRow = false,
                    numberRowMode = 0,
                    useLocalNumbers = false,
                    arrowRow = false,
                    alternativePeriodKey = false,
                    bottomActionKey = null
                )
            )

            layoutSet.getKeyboard(
                KeyboardLayoutElement(
                    kind = KeyboardLayoutKind.Alphabet0,
                    page = KeyboardLayoutPage.Base
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create spell checker keyboard for $locale", e)
            null
        }
    }
}
