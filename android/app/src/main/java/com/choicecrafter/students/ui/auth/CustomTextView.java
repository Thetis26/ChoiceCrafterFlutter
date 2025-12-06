package com.choicecrafter.studentapp.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;

import com.choicecrafter.studentapp.R;

public class CustomTextView extends androidx.appcompat.widget.AppCompatTextView {

    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Call the super implementation, which generates an accessibility event
        super.performClick();
        // Handle the click here
        Log.d("CustomTextView", "TextView clicked");
        // Start the activity
        Context context = getContext();
        Intent intent = null;
        int viewId = getId();

        if (viewId == R.id.toRegistration) {
            intent = new Intent(context, RegisterActivity.class);
        } else if (viewId == R.id.toLogin) {
            intent = new Intent(context, LoginActivity.class);
        }

        if (intent != null && context != null) {
            context.startActivity(intent);
        } else {
            Log.w("CustomTextView", "No navigation target found for view id: " + viewId);
        }
        return true;
    }
}
