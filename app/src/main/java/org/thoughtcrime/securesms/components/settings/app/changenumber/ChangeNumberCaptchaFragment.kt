/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.changenumber

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.theme.SignalTheme
import org.signal.core.ui.logging.LoggingFragment
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ViewBinderDelegate
import org.thoughtcrime.securesms.databinding.FragmentRegistrationCaptchaBinding
import org.thoughtcrime.securesms.registration.fragments.RegistrationConstants
import org.thoughtcrime.securesms.util.SystemWindowInsetsSetter
import pigeon.compose.PigeonChangeNumberCaptchaScreen
import pigeon.extensions.isPigeonVersion

/**
 * Screen visible to the user when they are to solve a captcha.
 */
class ChangeNumberCaptchaFragment : LoggingFragment() {
  private val viewModel by activityViewModels<ChangeNumberViewModel>()
  private val binding: FragmentRegistrationCaptchaBinding by ViewBinderDelegate(FragmentRegistrationCaptchaBinding::bind)

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    if (isPigeonVersion()) {
      return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
          SignalTheme {
            PigeonChangeNumberCaptchaScreen(
              onCaptchaCompleted = { token ->
                viewModel.setCaptchaResponse(token)
                findNavController().navigateUp()
              }
            )
          }
        }
      }
    }

    return inflater.inflate(R.layout.fragment_registration_captcha, container, false)
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (isPigeonVersion()) {
      return
    }

    SystemWindowInsetsSetter.attach(view, viewLifecycleOwner)
    binding.registrationCaptchaWebView.settings.javaScriptEnabled = true
    binding.registrationCaptchaWebView.clearCache(true)
    binding.registrationCaptchaWebView.webViewClient = object : WebViewClient() {
      @Deprecated("Deprecated in Java")
      override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (url.startsWith(RegistrationConstants.SIGNAL_CAPTCHA_SCHEME)) {
          viewModel.setCaptchaResponse(url.substring(RegistrationConstants.SIGNAL_CAPTCHA_SCHEME.length))
          findNavController().navigateUp()
          return true
        }
        return false
      }
    }
    binding.registrationCaptchaWebView.loadUrl(BuildConfig.SIGNAL_CAPTCHA_URL)
  }

}

