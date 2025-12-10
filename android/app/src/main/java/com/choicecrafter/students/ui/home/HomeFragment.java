package com.choicecrafter.students.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.choicecrafter.students.MainViewModel;
import com.choicecrafter.students.R;
import com.choicecrafter.students.models.User;

public class HomeFragment extends Fragment {

    private MainViewModel mainViewModel;
    private User loggedInUser;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        FrameLayout frameLayout = view.findViewById(R.id.frameLayout);

        // Load CoursesFragment by default
        if (savedInstanceState == null) {
            CoursesFragment coursesFragment = new CoursesFragment();
            loadFragment(coursesFragment);
        }
        if (mainViewModel != null) {
            mainViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
                loggedInUser = user;
                if (user != null && user.getName() != null) {
                    requireActivity().setTitle("Hello, " + user.getName());
                } else {
                    requireActivity().setTitle(getString(R.string.app_name));
                }
            });
        }
        return view;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        transaction.commit();
    }
}