/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.latin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;

/**
 * Cache for dictionary facilitators of multiple locales.
 * This class automatically creates and releases up to 3 facilitator instances using LRU policy.
 */
public class DictionaryFacilitatorLruCache {
    private static final String TAG = "DictionaryFacilitatorLruCache";
    private static final int WAIT_FOR_LOADING_MAIN_DICT_IN_MILLISECONDS = 1000;
    private static final int MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT = 5;

    private static final int MAX_CACHED_FACILITATORS = 4;

    private final Context mContext;
    private final String mDictionaryNamePrefix;
    private final Object mLock = new Object();
    // One facilitator per locale list, so that checking words of several languages doesn't keep
    // reloading dictionaries, and a facilitator never changes locale while another thread uses it.
    private final LinkedHashMap<List<Locale>, DictionaryFacilitator> mFacilitators =
            new LinkedHashMap<>(8, 0.75f, true /* access order */);
    private boolean mUseContactsDictionary;

    public DictionaryFacilitatorLruCache(final Context context, final String dictionaryNamePrefix) {
        mContext = context;
        mDictionaryNamePrefix = dictionaryNamePrefix;
    }

    private static void waitForLoadingMainDictionary(
            final DictionaryFacilitator dictionaryFacilitator) {
        for (int i = 0; i < MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT; i++) {
            try {
                dictionaryFacilitator.waitForLoadingMainDictionaries(
                        WAIT_FOR_LOADING_MAIN_DICT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
                return;
            } catch (final InterruptedException e) {
                Log.i(TAG, "Interrupted during waiting for loading main dictionary.", e);
                if (i < MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT - 1) {
                    Log.i(TAG, "Retry", e);
                } else {
                    Log.w(TAG, "Give up retrying. Retried "
                            + MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT + " times.", e);
                }
            }
        }
    }

    private void resetDictionariesLocked(final DictionaryFacilitator facilitator,
            final List<Locale> locales) {
        // Note: Given that personalized dictionaries are not used here; we can pass null account.
        facilitator.resetDictionaries(mContext, locales,
                mUseContactsDictionary, false /* usePersonalizedDicts */,
                false /* forceReloadMainDictionary */, null /* account */,
                mDictionaryNamePrefix, null /* listener */);
    }

    public void setUseContactsDictionary(final boolean useContactsDictionary) {
        synchronized (mLock) {
            if (mUseContactsDictionary == useContactsDictionary) {
                // The value has not been changed.
                return;
            }
            mUseContactsDictionary = useContactsDictionary;
            for (final Map.Entry<List<Locale>, DictionaryFacilitator> entry
                    : mFacilitators.entrySet()) {
                resetDictionariesLocked(entry.getValue(), entry.getKey());
                waitForLoadingMainDictionary(entry.getValue());
            }
        }
    }

    public DictionaryFacilitator get(final List<Locale> locales) {
        synchronized (mLock) {
            DictionaryFacilitator facilitator = mFacilitators.get(locales);
            if (facilitator == null) {
                facilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(
                        true /* isNeededForSpellChecking */);
                final List<Locale> key = new ArrayList<>(locales);
                resetDictionariesLocked(facilitator, key);
                mFacilitators.put(key, facilitator);
                if (mFacilitators.size() > MAX_CACHED_FACILITATORS) {
                    final List<Locale> eldestKey = mFacilitators.keySet().iterator().next();
                    mFacilitators.remove(eldestKey).closeDictionaries();
                }
            }
            waitForLoadingMainDictionary(facilitator);
            return facilitator;
        }
    }

    public void closeDictionaries() {
        synchronized (mLock) {
            for (final DictionaryFacilitator facilitator : mFacilitators.values()) {
                facilitator.closeDictionaries();
            }
            mFacilitators.clear();
        }
    }
}
