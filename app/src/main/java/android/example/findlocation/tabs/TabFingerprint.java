package android.example.findlocation.tabs;

import android.example.findlocation.R;
import android.example.findlocation.activities.OfflineTabedActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import org.w3c.dom.Text;

import java.util.HashMap;

public class TabFingerprint extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tabfingerprint, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView mSelectedTypes = view.findViewById(R.id.types_selectedId);
        mSelectedTypes.setText(getResources().getString(R.string.selected_sensor_types_string,((OfflineTabedActivity) getActivity()).getSelectedTypes()));
        TextView mSelectedPreferences = view.findViewById(R.id.defaultpreferencesId);
        mSelectedPreferences.setText(getResources().getString(R.string.general_preferences_string,((OfflineTabedActivity) getActivity()).getPreferences()));
        computeBulletList(view);
    }

    public void computeBulletList(View view) {
        String longDescription = "Go to the position you want to scan for fingerprints.\n" +
                "Click on the button \"ADD FINGERPRINTS\"\n" +
                "Wait until the scan is over\n" +
                "Go to the next position and start again";

        TextView description = (TextView) view.findViewById(R.id.fingerprintprocessId);

        String arr[] = longDescription.split("\n");

        int bulletGap = (int) dp(10);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = 0; i < arr.length; i++) {
            String line = arr[i];
            SpannableString ss = new SpannableString(line);
            ss.setSpan(new BulletSpan(bulletGap), 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(ss);

            //avoid last "\n"
            if (i + 1 < arr.length)
                ssb.append("\n");
        }

        description.setText(ssb);
    }
    private float dp(int dp) {
        return getResources().getDisplayMetrics().density * dp;
    }
}
