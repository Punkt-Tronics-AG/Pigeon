package pigeon.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.thoughtcrime.securesms.R;

public class DisclaimerFragment extends Fragment {

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_registration_disclaimer, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TextView mDisclaimerTV = view.findViewById(R.id.disclaimer_tv);

    StringBuilder builder = new StringBuilder();
    builder.append(getString(R.string.disclaimer_1));
    builder.append(getString(R.string.disclaimer_2));
    builder.append(getString(R.string.disclaimer_3));
    builder.append(getString(R.string.disclaimer_4));
    builder.append(getString(R.string.disclaimer_5));
    builder.append(getString(R.string.disclaimer_6));
    builder.append(getString(R.string.disclaimer_7));
    builder.append(getString(R.string.disclaimer_8));
    builder.append(getString(R.string.disclaimer_9));
    mDisclaimerTV.setText(builder);
  }
}