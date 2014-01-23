package com.yaohailong.lutedroid;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author Yaohailong
 *
 */
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	
	String IP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void onTabSelected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
		mViewPager.setCurrentItem(tab.getPosition());
	}
	@Override
	public void onTabUnselected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
	}
	@Override
	public void onTabReselected(ActionBar.Tab tab,FragmentTransaction fragmentTransaction) {
	}

	//分頁layout的承接器
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		private SparseArray<Fragment> map = new SparseArray<Fragment>();

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}
		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {
			case 0:
				fragment = new ConnectFragment();
				break;
			case 1:
				fragment = new SettingFragment();
				break;
			case 2:
				fragment = new AboutFragment();
				break;
			}
			map.put(position, fragment);
			return fragment;
		}
		@Override
		public int getCount() {
			return 3;
		}
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_section1);
			case 1:
				return getString(R.string.title_section2);
			case 2:
				return getString(R.string.title_section3);
			}
			return null;
		}
		
		public Fragment getFragment(int position){
			  return map.get(position);
		}
		
	}
	
	//各分頁定義
	public static class ConnectFragment extends Fragment {
		public ConnectFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_connect,container, false);
			EditText et_IP = (EditText)view.findViewById(R.id.et_IP);
			Button b_start = (Button)view.findViewById(R.id.b_start);
			Button b_stop = (Button)view.findViewById(R.id.b_stop);
			b_start.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Log.d("XXXXX","b_start clicked");
				}
			});
			return view;
		}
	}

	public static class SettingFragment extends Fragment {
		public SettingFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_setting,container, false);
			return view;
		}
	}
	
	public static class AboutFragment extends Fragment {
		public AboutFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_about,container, false);
			return view;
		}
	}
}
