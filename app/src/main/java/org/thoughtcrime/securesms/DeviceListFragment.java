package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.loaders.DeviceListLoader;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.devicelist.Device;
import org.thoughtcrime.securesms.jobs.LinkedDeviceInactiveCheckJob;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pigeon.components.Mp02CustomDialog;

import static pigeon.extensions.BuildExtensionsKt.isPigeonVersion;
import static pigeon.extensions.BuildExtensionsKt.isSignalVersion;
import static pigeon.extensions.KotilinExtensionsKt.focusOnLeft;

public class DeviceListFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<List<Device>>,
               ListView.OnItemClickListener, Button.OnClickListener
{

  private static final String TAG = Log.tag(DeviceListFragment.class);

  private SignalServiceAccountManager accountManager;
  private Locale                      locale;
  private View                        empty;
  private View                        progressContainer;
  private FloatingActionButton        addDeviceButton;
  private Button.OnClickListener      addDeviceButtonListener;

  private TextView             mAddDeviceView;
  private View.OnClickListener mAddDeviceViewListener;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.locale = (Locale) requireArguments().getSerializable(PassphraseRequiredActivity.LOCALE_EXTRA);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    this.accountManager = ApplicationDependencies.getSignalServiceAccountManager();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View view = inflater.inflate(R.layout.device_list_fragment, container, false);

    this.progressContainer = view.findViewById(R.id.progress_container);

    if (isSignalVersion()) {
      this.empty             = view.findViewById(R.id.empty);
      this.progressContainer = view.findViewById(R.id.progress_container);
      this.addDeviceButton   = view.findViewById(R.id.add_device);
      this.addDeviceButton.setOnClickListener(this);
    }

    if (isPigeonVersion()) {
      this.mAddDeviceView = view.findViewById(R.id.link_device_nav);
      focusOnLeft(this.mAddDeviceView);
      this.mAddDeviceView.requestFocus();
      this.mAddDeviceView.setOnClickListener(this);
    }

    return view;
  }

  @Override
  public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);
    getLoaderManager().initLoader(0, null, this);
    getListView().setOnItemClickListener(this);
  }

  public void setAddDeviceButtonListener(Button.OnClickListener listener) {
    this.addDeviceButtonListener = listener;
  }

  public void setAddDeviceViewListener(View.OnClickListener listener) {
    this.mAddDeviceViewListener = listener;
  }

  @Override
  public @NonNull Loader<List<Device>> onCreateLoader(int id, Bundle args) {
    if (isSignalVersion())
      empty.setVisibility(View.GONE);
    progressContainer.setVisibility(View.VISIBLE);
    return new DeviceListLoader(getActivity(), accountManager);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<List<Device>> loader, List<Device> data) {
    progressContainer.setVisibility(View.GONE);

    if (data == null) {
      handleLoaderFailed();
      return;
    }

    setListAdapter(new DeviceListAdapter(getActivity(), R.layout.device_list_item_view, data, locale));

    if (isSignalVersion()) {
      if (data.isEmpty()) {
        empty.setVisibility(View.VISIBLE);
        TextSecurePreferences.setMultiDevice(getActivity(), false);
        SignalStore.misc().setHasLinkedDevices(false);
      } else {
        SignalStore.misc().setHasLinkedDevices(true);
        empty.setVisibility(View.GONE);
      }
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<List<Device>> loader) {
    setListAdapter(null);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    final String deviceName = ((DeviceListItem) view).getDeviceName();
    final long   deviceId   = ((DeviceListItem) view).getDeviceId();

    if (isSignalVersion()) {
      AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
      builder.setTitle(getString(R.string.DeviceListActivity_unlink_s, deviceName));
      builder.setMessage(R.string.DeviceListActivity_by_unlinking_this_device_it_will_no_longer_be_able_to_send_or_receive);
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, (dialog, which) -> handleDisconnectDevice(deviceId));
      builder.show();
    }

    if (isPigeonVersion()) {
      final String titleText = getString(R.string.DeviceListActivity_unlink_s, deviceName);
      final String bodyText  = getString(R.string.DeviceListActivity_by_unlinking_this_device_it_will_no_longer_be_able_to_send_or_receive);

      Mp02CustomDialog dialog = new Mp02CustomDialog(requireContext());
      dialog.setMessage(titleText + '\n' + bodyText);
      dialog.setNegativeListener(android.R.string.cancel, null);
      dialog.setPositiveListener(android.R.string.ok, () -> {
        handleDisconnectDevice(deviceId);
        dialog.dismiss();
      });
      dialog.show();
    }
  }

  private void handleLoaderFailed() {
    if (isSignalVersion()) {
      AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
      builder.setMessage(R.string.DeviceListActivity_network_connection_failed);
      builder.setPositiveButton(R.string.DeviceListActivity_try_again,
                                (dialog, which) -> getLoaderManager().restartLoader(0, null, DeviceListFragment.this));

      builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> requireActivity().onBackPressed());
      builder.setOnCancelListener(dialog -> requireActivity().onBackPressed());

      builder.show();
    }

    if (isPigeonVersion()) {
      Mp02CustomDialog dialog = new Mp02CustomDialog(requireContext());
      dialog.setMessage(getString(R.string.DeviceListActivity_network_failed));
      dialog.setPositiveListener(R.string.DeviceListActivity_try_again, () -> {
        getLoaderManager().restartLoader(0, null, DeviceListFragment.this);
      });

      dialog.setNegativeListener(android.R.string.cancel, () -> {
        requireActivity().onBackPressed();
      });
      dialog.setBackKeyListener(() -> {
        requireActivity().onBackPressed();
      });

      dialog.show();
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void handleDisconnectDevice(final long deviceId) {
    new ProgressDialogAsyncTask<Void, Void, Boolean>(getActivity(),
                                                     R.string.DeviceListActivity_unlinking_device_no_ellipsis,
                                                     R.string.DeviceListActivity_unlinking_device)
    {
      @Override
      protected Boolean doInBackground(Void... params) {
        try {
          accountManager.removeDevice(deviceId);
          return true;
        } catch (IOException e) {
          Log.w(TAG, e);
          return false;
        }
      }

      @Override
      protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
          getLoaderManager().restartLoader(0, null, DeviceListFragment.this);
          LinkedDeviceInactiveCheckJob.enqueue();
        } else {
          Toast.makeText(getActivity(), R.string.DeviceListActivity_network_failed, Toast.LENGTH_LONG).show();
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public void onClick(View v) {
    if (isSignalVersion())
      if (addDeviceButtonListener != null) addDeviceButtonListener.onClick(v);

    if (isPigeonVersion())
      if (mAddDeviceViewListener != null) mAddDeviceViewListener.onClick(v);
  }

  private static class DeviceListAdapter extends ArrayAdapter<Device> {

    private final int    resource;
    private final Locale locale;

    public DeviceListAdapter(Context context, int resource, List<Device> objects, Locale locale) {
      super(context, resource, objects);
      this.resource = resource;
      this.locale   = locale;
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
      if (convertView == null) {
        convertView = ((Activity) getContext()).getLayoutInflater().inflate(resource, parent, false);
      }

      ((DeviceListItem) convertView).set(getItem(position), locale);

      return convertView;
    }
  }
}
