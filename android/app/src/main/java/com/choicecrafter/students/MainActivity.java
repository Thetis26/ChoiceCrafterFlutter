package com.choicecrafter.students;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.analytics.WeeklyUsageExportWorker;
import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.NudgePreferences;
import com.choicecrafter.students.models.User;
import com.choicecrafter.students.notifications.ChatNotificationListener;
import com.choicecrafter.students.notifications.MessagingTokenManager;
import com.choicecrafter.students.notifications.MotivationalReminderWorker;
import com.choicecrafter.students.repositories.FirestoreListener;
import com.choicecrafter.students.repositories.NudgePreferencesRepository;
import com.choicecrafter.students.ui.auth.LoginActivity;
import com.choicecrafter.students.utils.AppLogger;
import com.choicecrafter.students.utils.Avatar;
import com.choicecrafter.students.utils.DeviceSecurityPatchInspector;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.BuildConfig;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.choicecrafter.students.repositories.CourseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends FragmentActivity {
    private static final String KEY_LOGGED_IN_USER = "key_logged_in_user";
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private MaterialToolbar toolbar;
    private View bottomBarContainer;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private User loggedInUser;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MainViewModel mainViewModel;
    private FirestoreListener firestoreListener;
    private String firestoreListenerUserId;
    private ListenerRegistration nudgePreferencesRegistration;
    private final NudgePreferencesRepository nudgePreferencesRepository = new NudgePreferencesRepository();
    private NudgePreferences currentNudgePreferences;
    private final CourseRepository courseRepository = new CourseRepository();
    private String pendingCourseNavigationId;
    private String pendingHighlightActivityId;
    private String pendingHighlightActivityTitle;
    private boolean isResolvingCourseNavigation;
    private ChatNotificationListener chatNotificationListener;

    private final Set<Integer> topLevelDestinations = new HashSet<>();
    private final Set<Integer> destinationsWithoutBottomBar = new HashSet<>(Arrays.asList(
            R.id.courseActivities,
            R.id.activityFragment,
            R.id.moduleFragment,
            R.id.learningPathDetailFragment,
            R.id.recommendationWebViewFragment,
            R.id.feedbackFragment
    ));

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        AppLogger.d(TAG, "Processing incoming intent", "extras", intent.getExtras());

        if (intent.getBooleanExtra("openCourseActivities", false)) {
            String courseId = intent.getStringExtra("courseId");
            String highlightActivityId = intent.getStringExtra("highlightActivityId");
            intent.removeExtra("openCourseActivities");

            setPendingCourseNavigation(courseId, highlightActivityId, null);
            resolvePendingCourseNavigation();
        }

        if (intent.getBooleanExtra("openActivityFragment", false)) {
            Activity activity = intent.getParcelableExtra("activity");
            String courseId = intent.getStringExtra("courseId");
            if (activity != null) {
                setPendingCourseNavigation(courseId, activity.getId(), activity.getTitle());
                resolvePendingCourseNavigation();
            }
        }
    }

    private void setPendingCourseNavigation(String courseId, String highlightActivityId, String highlightActivityTitle) {
        pendingCourseNavigationId = courseId;
        pendingHighlightActivityId = highlightActivityId;
        pendingHighlightActivityTitle = highlightActivityTitle;
    }

    private void resolvePendingCourseNavigation() {
        if (TextUtils.isEmpty(pendingCourseNavigationId) || navController == null || isResolvingCourseNavigation) {
            return;
        }
        String userEmail = loggedInUser != null ? loggedInUser.getEmail() : null;
        if (TextUtils.isEmpty(userEmail)) {
            return;
        }
        isResolvingCourseNavigation = true;
        courseRepository.getCourseById(pendingCourseNavigationId, new CourseRepository.Callback<>() {
            @Override
            public void onSuccess(Course course) {
                isResolvingCourseNavigation = false;
                if (course == null) {
                    AppLogger.w(TAG, "Received null course while resolving navigation", "courseId", pendingCourseNavigationId);
                    clearPendingCourseNavigation();
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable("course", course);
                bundle.putString("userId", userEmail);
                String highlightKey = !TextUtils.isEmpty(pendingHighlightActivityId)
                        ? pendingHighlightActivityId
                        : pendingHighlightActivityTitle;
                if (!TextUtils.isEmpty(highlightKey)) {
                    bundle.putString("highlightActivityId", highlightKey);
                }
                try {
                    navController.navigate(R.id.courseActivities, bundle);
                } catch (IllegalArgumentException | IllegalStateException navigationException) {
                    AppLogger.w(TAG, "Failed to navigate to course activities", navigationException,
                            "courseId", pendingCourseNavigationId);
                }
                clearPendingCourseNavigation();
            }

            @Override
            public void onFailure(Exception e) {
                isResolvingCourseNavigation = false;
                AppLogger.e(TAG, "Failed to load course for navigation", e, "courseId", pendingCourseNavigationId);
                clearPendingCourseNavigation();
            }
        });
    }

    private void clearPendingCourseNavigation() {
        pendingCourseNavigationId = null;
        pendingHighlightActivityId = null;
        pendingHighlightActivityTitle = null;
    }

    private void applySavedLocale() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        AppLogger.d(TAG, "Applying saved locale", "language", lang);
        setLocale(lang);
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        try (AppLogger.TraceSession ignored = AppLogger.trace(TAG, "onCreate", "hasSavedState", savedInstanceState != null)) {
            applySavedLocale();
            FirebaseApp.initializeApp(this);
            initializeFirebaseAppCheck();
            // It's safe to schedule workers here
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WeeklyUsageExportWorker.schedule(getApplicationContext());
                MotivationalReminderWorker.schedule(getApplicationContext());
            }
            requestNotificationPermissionIfNeeded();

            setContentView(R.layout.activity_main);
            toolbar = findViewById(R.id.toolbar);
            drawerLayout = findViewById(R.id.drawer_layout);
            bottomBarContainer = findViewById(R.id.app_bar_main);
            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            navigationView = findViewById(R.id.nav_view);
            mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }

            topLevelDestinations.addAll(Arrays.asList(
                    R.id.home,
                    R.id.subscriptions,
                    R.id.library,
                    R.id.inboxFragment,
                    R.id.messagesFragment,
                    R.id.shorts
            ));

            appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                    .setOpenableLayout(drawerLayout)
                    .build();

            if (toolbar != null && navController != null) {
                NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
            }
            if (bottomNavigationView != null && navController != null) {
                NavigationUI.setupWithNavController(bottomNavigationView, navController);
            }
            if (navigationView != null && navController != null) {
                NavigationUI.setupWithNavController(navigationView, navController);
            }

            if (drawerLayout != null && toolbar != null) {
                toggle = new ActionBarDrawerToggle(
                        this,
                        drawerLayout,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close
                );
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }

            if (navController != null) {
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    boolean showBottomBar = !destinationsWithoutBottomBar.contains(destination.getId());
                    if (bottomBarContainer != null) {
                        bottomBarContainer.setVisibility(showBottomBar ? View.VISIBLE : View.GONE);
                    }
                    if (topLevelDestinations.contains(destination.getId())) {
                        showHamburger();
                    } else {
                        hideHamburger();
                    }
                });
            }

            if (navigationView != null) {
                findLoggedInUserData(navigationView);
            }

            if (savedInstanceState != null) {
                loggedInUser = savedInstanceState.getParcelable(KEY_LOGGED_IN_USER);
                AppLogger.d(TAG, "Restored logged in user from saved state", "hasUser", loggedInUser != null);
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.i(TAG, "Notification permission granted by the user");
            } else {
                AppLogger.w(TAG, "Notification permission denied. Activity alerts will not be shown.");
                Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeFirebaseAppCheck() {
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        int playServicesStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (!DeviceSecurityPatchInspector.hasValidVendorPatchLevel()) {
            String vendorPatch = DeviceSecurityPatchInspector.getVendorSecurityPatch();
            AppLogger.w(TAG, "Invalid or missing vendor security patch level detected. Skipping Play Integrity App Check to avoid hardware attestation failures.",
                    "vendorPatch", vendorPatch);
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                );
                AppLogger.w(TAG, "App is running in debug mode. Using Debug App Check provider as a fallback.");
            }
            return;
        }

        if (playServicesStatus == ConnectionResult.SUCCESS) {
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        } else if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
            AppLogger.w(TAG, "Google Play services unavailable, using Debug App Check provider",
                    "statusCode", playServicesStatus);
        } else {
            AppLogger.w(TAG, "Google Play services unavailable on device. App Check initialization skipped.",
                    "statusCode", playServicesStatus);
        }
    }

    public void showHamburger() {
        // Ensure the drawer can be opened by swiping
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }

        if (toolbar != null) {
            // Use the ActionBarDrawerToggle to handle the icon and clicks
            if (toggle != null) {
                // This automatically sets the hamburger icon and syncs its state
                toggle.setDrawerIndicatorEnabled(true);
                // This sets the correct click listener to open/close the drawer
                toolbar.setNavigationOnClickListener(toggle.getToolbarNavigationClickListener());
            }
        }
    }

    public void hideHamburger() {
        // Lock the drawer so it cannot be opened by swiping
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        if (toolbar != null) {
            // Disable the ActionBarDrawerToggle's indicator
            if (toggle != null) {
                toggle.setDrawerIndicatorEnabled(false);
            }
            // Set the back arrow icon manually
            toolbar.setNavigationIcon(R.drawable.ic_back_arrow);
            // Set the click listener to handle "Up" navigation
            toolbar.setNavigationOnClickListener(v -> handleToolbarNavigateUp());
        }
    }


    private void handleToolbarNavigateUp() {
        if (navController != null && navController.navigateUp()) {
            return;
        }
        super.onBackPressed();
    }

    private void updateNavigationHeader(NavigationView navigationView, User user) {
        View headerView = navigationView.getHeaderView(0);
        TextView emailTextView = headerView.findViewById(R.id.userEmail);
        TextView nameTextView = headerView.findViewById(R.id.userName);
        ShapeableImageView profileImageView = headerView.findViewById(R.id.profileImage);

        if (user == null) {
            loggedInUser = null;
            if (mainViewModel != null) {
                mainViewModel.setUser(null);
            }
            setTitle("Hello, User");
            emailTextView.setText("");
            nameTextView.setText("User");
            profileImageView.setImageResource(R.drawable.avatar1);
            stopFirestoreListener();
            MessagingTokenManager.clearSyncedUser(getApplicationContext());
            return;
        }

        loggedInUser = user;
        if (mainViewModel != null) {
            mainViewModel.setUser(user);
        }
        MessagingTokenManager.syncTokenIfNecessary(getApplicationContext(), user.getEmail());
        String username = user.getName();
        if (username != null && !username.isEmpty()) {
            setTitle("Hello, " + username);
            nameTextView.setText(username);
        } else {
            setTitle("Hello, User");
            nameTextView.setText("User");
        }
        emailTextView.setText(user.getEmail() != null ? user.getEmail() : "");

        String imageUrl = user.getAnonymousAvatar() != null ? user.getAnonymousAvatar().getImageUrl() : null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(profileImageView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.avatar1)
                    .error(R.drawable.avatar1)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.avatar1);
        }

        startFirestoreListenerForUser(user);
    }

    private void findLoggedInUserData(NavigationView navigationView) {
        if (loggedInUser != null) {
            updateNavigationHeader(navigationView, loggedInUser);
            handleIntent(getIntent());
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            AppLogger.e(TAG, "No authenticated user found while trying to load profile");
            updateNavigationHeader(navigationView, null);
            handleIntent(getIntent());
            return;
        }

        String userEmail = firebaseUser.getEmail();
        if (userEmail == null) {
            AppLogger.e(TAG, "Authenticated Firebase user has null email");
            updateNavigationHeader(navigationView, null);
            handleIntent(getIntent());
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    AppLogger.d(TAG, "User lookup completed",
                            "success", task.isSuccessful(),
                            "resultCount", task.getResult() != null ? task.getResult().size() : 0);
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Map<String, Long> scores = new HashMap<>();
                        Object scoresObj = task.getResult().getDocuments().get(0).get("scores");
                        if (scoresObj instanceof Map) {
                            Map<?, ?> rawMap = (Map<?, ?>) scoresObj;
                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                String key = String.valueOf(entry.getKey());
                                String valueStr = String.valueOf(entry.getValue());
                                try {
                                    scores.put(key, Long.parseLong(valueStr));
                                } catch (NumberFormatException e) {
                                    AppLogger.w(TAG, "Invalid score entry encountered while parsing user data",
                                            e, "key", key, "value", valueStr);
                                }
                            }
                        }

                        Object avatarObj = task.getResult().getDocuments().get(0).get("anonymousAvatar");
                        String anonymousName = null;
                        String anonymousImageUrl = null;
                        if (avatarObj instanceof Map) {
                            Map<?, ?> avatarMap = (Map<?, ?>) avatarObj;
                            Object nameObj = avatarMap.get("name");
                            Object imageUrlObj = avatarMap.get("imageUrl");
                            if (nameObj != null) {
                                anonymousName = nameObj.toString();
                            }
                            if (imageUrlObj != null) {
                                anonymousImageUrl = imageUrlObj.toString();
                            }
                        }

                        String username = task.getResult().getDocuments().get(0).getString("name");
                        Object badgesObj = task.getResult().getDocuments().get(0).get("badges");
                        List<String> badges = new ArrayList<>();
                        if (badgesObj instanceof List<?> list) {
                            for (Object value : list) {
                                if (value != null) {
                                    badges.add(String.valueOf(value));
                                }
                            }
                        }

                        User user = new User(username != null ? username : "User", userEmail, new Avatar(anonymousName, anonymousImageUrl), scores);
                        user.setBadges(badges);
                        AppLogger.i(TAG, "Retrieved logged in user profile", "email", userEmail,
                                "hasScores", !scores.isEmpty(),
                                "hasAvatar", anonymousImageUrl != null);
                        updateNavigationHeader(navigationView, user);
                    } else {
                        AppLogger.e(TAG, "Failed to find user by email", task.getException(), "email", userEmail);
                        updateNavigationHeader(navigationView, null);
                    }
                    handleIntent(getIntent());
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (loggedInUser != null) {
            outState.putParcelable(KEY_LOGGED_IN_USER, loggedInUser);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFirestoreListener();
    }

    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void startFirestoreListenerForUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            AppLogger.w(TAG, "Skipping Firestore listener start due to missing user information");
            stopFirestoreListener();
            return;
        }

        String email = user.getEmail();
        if (email.equals(firestoreListenerUserId)) {
            AppLogger.d(TAG, "Firestore listener already running for user", "email", email);
            return;
        }

        stopFirestoreListener();
        AppLogger.d(TAG, "Starting Firestore listener for user", "email", email);
        firestoreListener = new FirestoreListener(this, email);
        firestoreListener.startListeningForActivityStatusChanges();
        if (currentNudgePreferences != null) {
            firestoreListener.setActivityStartedNotificationsEnabled(
                    currentNudgePreferences.isActivityStartedNotificationsEnabled());
            firestoreListener.setDiscussionForumEnabled(currentNudgePreferences.isDiscussionForumEnabled());
        }
        firestoreListenerUserId = email;
        startChatNotificationListener(email);
        startNudgePreferencesListener(email);
    }

    private void stopFirestoreListener() {
        if (firestoreListener != null) {
            AppLogger.d(TAG, "Stopping Firestore listener", "previousUserId", firestoreListenerUserId);
            firestoreListener.stopListening();
            firestoreListener = null;
        }
        firestoreListenerUserId = null;
        stopChatNotificationListener();
        stopNudgePreferencesListener();
    }

    private void startNudgePreferencesListener(String userEmail) {
        if (TextUtils.isEmpty(userEmail)) {
            stopNudgePreferencesListener();
            return;
        }
        if (nudgePreferencesRegistration != null) {
            if (currentNudgePreferences != null && TextUtils.equals(currentNudgePreferences.getUserEmail(), userEmail)) {
                return;
            }
            stopNudgePreferencesListener();
        }
        nudgePreferencesRegistration = nudgePreferencesRepository.listenToPreferences(userEmail, preferences -> {
            currentNudgePreferences = preferences;
            if (mainViewModel != null) {
                mainViewModel.setNudgePreferences(preferences);
            }
            applyNudgePreferences(preferences);
        });
    }

    private void stopNudgePreferencesListener() {
        if (nudgePreferencesRegistration != null) {
            nudgePreferencesRegistration.remove();
            nudgePreferencesRegistration = null;
        }
        currentNudgePreferences = null;
        if (mainViewModel != null) {
            mainViewModel.setNudgePreferences(null);
        }
        updateColleaguesNavigation(true);
    }

    private void applyNudgePreferences(NudgePreferences preferences) {
        if (preferences == null) {
            return;
        }
        updateColleaguesNavigation(preferences.isColleaguesActivityPageEnabled());
        updateReminderWorker(preferences.isReminderNotificationsEnabled());
        if (firestoreListener != null) {
            firestoreListener.setActivityStartedNotificationsEnabled(preferences.isActivityStartedNotificationsEnabled());
            firestoreListener.setDiscussionForumEnabled(preferences.isDiscussionForumEnabled());
        }
    }

    private void updateColleaguesNavigation(boolean enabled) {
        if (bottomNavigationView != null) {
            MenuItem colleaguesItem = bottomNavigationView.getMenu().findItem(R.id.shorts);
            if (colleaguesItem != null) {
                colleaguesItem.setVisible(enabled);
            }
        }
        if (!enabled && navController != null && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.shorts) {
            navController.navigate(R.id.home);
        }
    }

    private void updateReminderWorker(boolean enabled) {
        if (enabled) {
            MotivationalReminderWorker.schedule(getApplicationContext());
        } else {
            WorkManager.getInstance(getApplicationContext())
                    .cancelUniqueWork(MotivationalReminderWorker.WORK_NAME);
        }
    }

    private void startChatNotificationListener(String userId) {
        if (chatNotificationListener == null) {
            chatNotificationListener = new ChatNotificationListener(getApplicationContext());
        }
        chatNotificationListener.start(userId);
    }

    private void stopChatNotificationListener() {
        if (chatNotificationListener != null) {
            chatNotificationListener.stop();
            chatNotificationListener = null;
        }
    }
}
