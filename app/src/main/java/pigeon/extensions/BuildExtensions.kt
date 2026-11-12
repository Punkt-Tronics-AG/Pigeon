package pigeon.extensions

import org.thoughtcrime.securesms.BuildConfig

fun isSignalVersion(): Boolean = BuildConfig.IS_SIGNAL

fun isPigeonVersion(): Boolean = !BuildConfig.IS_SIGNAL