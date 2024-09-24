package org.thoughtcrime.securesms.components.reminder;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.util.CommunicationActions;
import org.thoughtcrime.securesms.util.PlayStoreUtil;
import static pigeon.extensions.BuildExtensionsKt.*;

import java.util.List;

/**
 * Showed when a build has fully expired (either via the compile-time constant, or remote
 * deprecation).
 */
public class ExpiredBuildReminder extends Reminder {

  public static int getPigeonTitle(){
    if (isPigeonVersion()){
      return R.string.Pigeon_ExpiredBuildReminder_this_version_of_signal_has_expired;
    } else  {
      return R.string.ExpiredBuildReminder_this_version_of_signal_has_expired;
    }
  }

  public ExpiredBuildReminder(final Context context) {
//    super(R.string.ExpiredBuildReminder_this_version_of_signal_has_expired);

    super(getPigeonTitle());
    if (isSignalVersion()) {
      addAction(new Action(R.string.ExpiredBuildReminder_update_now, R.id.reminder_action_update_now));
    } else {
      addAction(new Action(R.string.Pigeon_ok, -1));
    }
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  @Override
  public @NonNull Importance getImportance() {
    return Importance.TERMINAL;
  }

  public static boolean isEligible() {
    return SignalStore.misc().isClientDeprecated() && isSignalVersion();
  }
}
