package android.example.findlocation.ui.adapters;

import android.content.Context;
import android.example.findlocation.R;
import android.example.findlocation.ui.tabs.TabProximityDistanceMain;
import android.example.findlocation.ui.tabs.TabProximityDistancePreferences;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class SectionsPagerAdapterProximityDistance extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{ R.string.tab_text_1_proximity_distance,R.string.tab_text_2_proximity_distance};
    private final Context mContext;

    public SectionsPagerAdapterProximityDistance(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0:
                TabProximityDistanceMain tab1 = new TabProximityDistanceMain();
                return tab1;
            case 1:
                TabProximityDistancePreferences tab2 = new TabProximityDistancePreferences();
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
