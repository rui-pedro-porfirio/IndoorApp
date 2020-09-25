package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.ui.tabs.TabOnlinePreferences;
import android.example.findlocation.ui.tabs.TabPosition;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapterOnline extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{ R.string.tab_text_1_online,R.string.tab_text_3};
    private final Context mContext;

    public SectionsPagerAdapterOnline(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0:
                TabPosition tab1 = new TabPosition();
                return tab1;
            case 1:
                TabOnlinePreferences tab2 = new TabOnlinePreferences();
                return tab2;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }


    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }
}