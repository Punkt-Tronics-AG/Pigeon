package org.thoughtcrime.securesms;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import pigeon.components.Mp02CustomDialog;

import static pigeon.extensions.BuildExtensionsKt.isPigeonVersion;
import static pigeon.extensions.BuildExtensionsKt.isSignalVersion;

public class DeviceLinkFragment extends Fragment implements View.OnClickListener {

  private LinearLayout        container;
  private LinkClickedListener linkClickedListener;
  private Uri                 uri;

  private EditText mUuidInput;
  private EditText mPubKeyInput;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
    this.container = (LinearLayout) inflater.inflate(R.layout.device_link_fragment, container, false);
    this.container.findViewById(R.id.link_device).setOnClickListener(this);
    if (isSignalVersion()) {
      ViewCompat.setTransitionName(container.findViewById(R.id.devices), "devices");

      if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        container.setOrientation(LinearLayout.HORIZONTAL);
      } else {
        container.setOrientation(LinearLayout.VERTICAL);
      }
    }

    if (isPigeonVersion()) {
      mUuidInput   = container.findViewById(R.id.device_uuid_input);
      mPubKeyInput = container.findViewById(R.id.device_pubkey_input);
    }

    return this.container;
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfiguration) {
    super.onConfigurationChanged(newConfiguration);
    if (newConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      container.setOrientation(LinearLayout.HORIZONTAL);
    } else {
      container.setOrientation(LinearLayout.VERTICAL);
    }
  }

  public void setLinkClickedListener(Uri uri, LinkClickedListener linkClickedListener) {
    this.uri                 = uri;
    this.linkClickedListener = linkClickedListener;
  }

  @Override
  public void onClick(View v) {
    if (linkClickedListener != null) {
      if (isSignalVersion())
        linkClickedListener.onLink(uri);

      if (isPigeonVersion()) {
        final String titleText   = getString(R.string.DeviceProvisioningActivity_link_this_device);
        final String introText   = getString(R.string.DeviceProvisioningActivity_content_intro);
        final String contentText = getString(R.string.DeviceProvisioningActivity_content_bullets);

        Mp02CustomDialog dialog = new Mp02CustomDialog(requireContext());
        dialog.setMessage(titleText + '\n' + introText + '\n' + contentText);
        dialog.setNegativeListener(android.R.string.no, null);
        dialog.setPositiveListener(android.R.string.yes, () -> {
          final String uuid   = mUuidInput.getText().toString();
          final String pubKey = mPubKeyInput.getText().toString();
          final String qrLink = String.format("linkdevice?uuid=%s&pub_key=%s", uuid, pubKey);
          linkClickedListener.onLink(uri != null ? uri : Uri.parse(qrLink));
        });
        dialog.show();
      }
    }
  }

  public interface LinkClickedListener {
    void onLink(Uri uri);
  }
}
