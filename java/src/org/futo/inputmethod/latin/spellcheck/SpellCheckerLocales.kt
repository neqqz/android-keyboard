package org.futo.inputmethod.latin.spellcheck

import android.content.Context
import android.util.Log
import org.futo.inputmethod.latin.Subtypes
import java.util.Locale

/**
 * The platform only knows FUTO's single static subtype, which has no locale, so it hands the spell
 * checker the system language. Like HeliBoard, use the language of the layout that is active in
 * FUTO instead.
 */
object SpellCheckerLocales {
    private const val TAG = "SpellCheckerLocales"

    /** Locale of the active FUTO layout, or null if it can't be determined. */
    @JvmStatic
    fun activeLocale(context: Context): Locale? {
        return try {
            Subtypes.getLocale(Subtypes.getActiveSubtype(context))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read active layout", e)
            null
        }
    }
}
