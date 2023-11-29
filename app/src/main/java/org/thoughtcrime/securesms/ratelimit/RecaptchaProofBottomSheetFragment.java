package org.thoughtcrime.securesms.ratelimit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.BottomSheetUtil;
import org.thoughtcrime.securesms.util.WindowUtil;
import static pigeon.extensions.BuildExtensionsKt.isPigeonVersion;
import static pigeon.extensions.KotilinExtensionsKt.focusOnLeft;

/**
 * A bottom sheet to be shown when we need to prompt the user to fill out a reCAPTCHA.
 */
public final class RecaptchaProofBottomSheetFragment extends BottomSheetDialogFragment {

  private static final String TAG = Log.tag(RecaptchaProofBottomSheetFragment.class);

  public static void show(@NonNull FragmentManager manager) {
    new RecaptchaProofBottomSheetFragment().show(manager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(dialogInterface -> {
      BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
      setupFullHeight(bottomSheetDialog);
    });
    return dialog;
  }

  private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
    FrameLayout bottomSheet = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
    assert bottomSheet != null;
    BottomSheetBehavior<FrameLayout> behavior     = BottomSheetBehavior.from(bottomSheet);
    ViewGroup.LayoutParams           layoutParams = bottomSheet.getLayoutParams();

    int windowHeight = getWindowHeight();
    if (layoutParams != null) {
      layoutParams.height = windowHeight;
    }
    bottomSheet.setLayoutParams(layoutParams);
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
  }

  private int getWindowHeight() {
    // Calculate window height for fullscreen use
    DisplayMetrics displayMetrics = new DisplayMetrics();
    ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.heightPixels;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    setStyle(DialogFragment.STYLE_NORMAL, R.style.Signal_DayNight_BottomSheet_Rounded);
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.recaptcha_required_bottom_sheet, container, false);

    view.findViewById(R.id.recaptcha_sheet_ok_button).setOnClickListener(v -> {
      dismissAllowingStateLoss();
      startActivity(RecaptchaProofActivity.getIntent(requireContext()));
    });

    focusOnLeft(view.findViewById(R.id.recaptcha_sheet_ok_button));

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    WindowUtil.initializeScreenshotSecurity(requireContext(), requireDialog().getWindow());
  }

  @Override
  public void show(@NonNull FragmentManager manager, @Nullable String tag) {
    Log.i(TAG, "Showing reCAPTCHA proof bottom sheet.");

    if (manager.findFragmentByTag(tag) == null) {
        BottomSheetUtil.show(manager, tag, this);
    } else {
      Log.i(TAG, "Ignoring repeat show.");
    }
  }
}
