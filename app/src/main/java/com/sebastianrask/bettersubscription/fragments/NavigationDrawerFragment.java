package com.sebastianrask.bettersubscription.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.AnimRes;
import androidx.annotation.IdRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.SearchActivity;
import com.sebastianrask.bettersubscription.activities.UsageTrackingAppCompatActivity;
import com.sebastianrask.bettersubscription.activities.main.FeaturedStreamsActivity;
import com.sebastianrask.bettersubscription.activities.main.MainActivity;
import com.sebastianrask.bettersubscription.activities.main.MyChannelsActivity;
import com.sebastianrask.bettersubscription.activities.main.MyGamesActivity;
import com.sebastianrask.bettersubscription.activities.main.MyStreamsActivity;
import com.sebastianrask.bettersubscription.activities.main.TopGamesActivity;
import com.sebastianrask.bettersubscription.activities.main.TopStreamsActivity;
import com.sebastianrask.bettersubscription.activities.settings.SettingsActivity;
import com.sebastianrask.bettersubscription.misc.TooltipWindow;
import com.sebastianrask.bettersubscription.service.DialogService;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.tasks.GetStreamsCountTask;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class NavigationDrawerFragment extends Fragment {
	private String LOG_TAG = getClass().getSimpleName();

	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;

	private Intent  mIntent;
	private Settings mSettings;

	@BindView(R.id.streams_count)
	protected TextView mStreamsCount;

	@BindView(R.id.streams_count_wrapper)
	protected FrameLayout mStreamsCountWrapper;

	@BindView(R.id.drawer_container)
	protected View containerView;

	@BindView(R.id.txt_app_name)
	protected TextView mAppTitleView;

	@BindView(R.id.txt_twitch_displayname)
	protected TextView mUserNameTextView;

	@BindView(R.id.img_app_icon)
	protected ImageView mAppIcon;

	@BindView(R.id.img_drawer_banner)
	protected ImageView mTopImage;

	@BindViews({ R.id.my_games_container, R.id.my_streams_container, R.id.my_channels_container})
	protected List<View> mUserRequiredViews;

	private TooltipWindow themeTip;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View mRoot = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
		ButterKnife.bind(this, mRoot);

		mSettings = new Settings(getActivity());

		initHeaderImage(mTopImage);
		fetchAndSetOnlineSteamsCount();

		return mRoot;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mDrawerLayout != null) { // If this layout isn't null then we know that the drawer has been setup
			checkUserLogin();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(themeTip != null && themeTip.isTooltipShown()){
			themeTip.dismissTooltip();
		}
	}

	private void fetchAndSetOnlineSteamsCount() {
		GetStreamsCountTask getStreamsCountTask = new GetStreamsCountTask(getContext(), new GetStreamsCountTask.Delegate() {
			@Override
			public void TaskFinished(int count) {
				if (count >= 0 && mStreamsCountWrapper != null && mStreamsCount != null) {
					showAndSetStreamCount(count);
				}
			}
		});
		getStreamsCountTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void showAndSetStreamCount(int count) {
		mStreamsCountWrapper.setVisibility(View.VISIBLE);
		Animation alphaAnimation = new AlphaAnimation(0f, 1f);
		alphaAnimation.setDuration(240);
		alphaAnimation.setFillAfter(true);
		mStreamsCountWrapper.startAnimation(alphaAnimation);
		mStreamsCount.setText(Integer.toString(count));
	}

	public void setUp(DrawerLayout drawerLayout, Toolbar toolbar) {
		mDrawerLayout = drawerLayout;

		// Create listener for changes in the nav drawer state.
		mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close){
			@Override
			public void onDrawerOpened(View drawerView) {
				if (mAppIcon == null) {
					return;
				}
				super.onDrawerOpened(drawerView);
				mAppIcon.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.anim_icon_rotation));
				checkForTip(mSettings, mAppTitleView);
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);

				if(mIntent != null) {
					if(getActivity() instanceof MainActivity) {
						MainActivity fromActivity = (MainActivity) getActivity();
						fromActivity.transitionToOtherMainActivity(mIntent);
					} else if (getContext() != null){
						ActivityCompat.startActivity(getContext(), mIntent, null);
					}
					mIntent = null;
				}
			}
		};

		// set the listener on the nav drawer
		mDrawerLayout.addDrawerListener(mDrawerToggle);

		// This simple method gives us the burger icon for the toolbar
		mDrawerLayout.post(new Runnable() {
			@Override
			public void run() {
				mDrawerToggle.syncState();
			}
		});

		setClickListeners();
		checkUserLogin();
	}

	private void setClickListeners() {
		// OnClick listeners for the items
		setOnClick(R.id.featured_streams_container, FeaturedStreamsActivity.class);
		setOnClick(R.id.top_streams_container, 		TopStreamsActivity.class);
		setOnClick(R.id.top_games_container, 		TopGamesActivity.class);
		setOnClick(R.id.my_channels_container, 		MyChannelsActivity.class);
		setOnClick(R.id.my_streams_container, 		MyStreamsActivity.class);
		setOnClick(R.id.my_games_container, 		MyGamesActivity.class);

		setInstantOnClick(R.id.search_container, 	SearchActivity.class, R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim);
		setInstantOnClick(R.id.settings_container, 	SettingsActivity.class, R.anim.slide_in_right_anim, R.anim.fade_out_semi_anim);
	}

	private void setInstantOnClick(@IdRes int viewRes, final Class activityClass, @AnimRes final int inAnimation, @AnimRes final int outAnimation) {
		View view = getActivity().findViewById(viewRes);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), activityClass);

				ActivityOptionsCompat searchAnim = ActivityOptionsCompat.makeCustomAnimation(getActivity(), inAnimation, outAnimation);
				ActivityCompat.startActivity(getActivity(), intent, searchAnim.toBundle());
				mDrawerLayout.closeDrawer(containerView);
			}
		});
	}

	private View setOnClick(@IdRes int viewID, Class aActivity) {
		View view = getActivity().findViewById(viewID);

		if(getActivity().getClass() == aActivity) {
			// Get the attribute highlight color
			TypedValue a = new TypedValue();
			getActivity().getTheme().resolveAttribute(R.attr.navigationDrawerHighlighted, a, true);
			if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
				int color = a.data;
				view.setBackgroundColor(color);
			}

			setCloseDrawerOnClick(view, mDrawerLayout, containerView);
		} else {
			setStandardOnClick(view, getActivity(), aActivity, mDrawerLayout, containerView);
		}

		return view;
	}

	private void setCloseDrawerOnClick(View mViewToListen, final DrawerLayout mDrawerLayout, final View mDrawerView) {
		mViewToListen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(getActivity() instanceof MainActivity) {
					((MainActivity) getActivity()).scrollToTopAndRefresh();
				} else {
					getActivity().recreate();
				}

				mDrawerLayout.closeDrawer(mDrawerView);
			}
		});
	}

	private void setStandardOnClick(View mViewToListen, final Activity mFromActivity, final Class mToClass, final DrawerLayout mDrawerLayout, final View mDrawerView) {
		mViewToListen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mFromActivity, mToClass);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // We don't want to use a transition animation

				mIntent = intent;

				// Close the drawer. This way the intent will be used to launch the next activity,
				// as the OnCloseListener will start the activity, now that the mIntent contains an actual reference
				mDrawerLayout.closeDrawer(mDrawerView);
			}
		});
	}

	private void checkUserLogin() {
		if (mSettings.isLoggedIn()) {
			mUserNameTextView.setText(getResources().getString(R.string.navigation_drawer_logged_in_textview, mSettings.getGeneralTwitchDisplayName()));
		} else {
			mUserNameTextView.setText(getString(R.string.navigation_drawer_not_logged_in));
		}

		if (!mSettings.isLoggedIn()) {
			for (View userView : mUserRequiredViews) {
				userView.setVisibility(View.GONE);
			}
		}
	}

	private void checkForTip(Settings settings, View Anchor) {
		try {
			themeTip = new TooltipWindow(getContext(),  TooltipWindow.POSITION_BOTTOM);
			if (!themeTip.isTooltipShown() && !settings.isTipsShown()) {
				themeTip.showToolTip(Anchor, getContext().getString(R.string.tip_theme));
				settings.setTipsShown(true);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to show NavigationDrawer ToolTip");
		}
	}

	public void initHeaderImage(final ImageView headerImageView) {
		headerImageView.setImageResource(R.drawable.nav_top);

		final MaterialDialog themeChooserDialog = DialogService.getThemeDialog(getActivity());
		headerImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				themeChooserDialog.show();
			}
		});
	}
}
