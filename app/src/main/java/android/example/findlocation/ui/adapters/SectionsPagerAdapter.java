package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.ui.tabs.TabFingerprint;
import android.example.findlocation.ui.tabs.TabPreferences;
import android.example.findlocation.ui.tabs.TabRadioMap;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3};
    private final Context mContext;
    private String radioMapTag;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                TabFingerprint tab1 = new TabFingerprint();
                return tab1;
            case 1:
                TabRadioMap tab2 = new TabRadioMap();
                radioMapTag = tab2.getTag();
                return tab2;
            case 2:
                TabPreferences tab3 = new TabPreferences();
                return tab3;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    public String getRadioMapTag() {
        return radioMapTag;
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 3;
    }
}