package iu.pervasive.autiapp.fragment;


import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import iu.pervasive.autiapp.R;
import ivb.com.materialstepper.stepperFragment;

public class UserInfo extends stepperFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(
                R.layout.fragment_user_info, container, false);

        Resources res = getResources(); //resource handle
        Drawable drawable = res.getDrawable(R.drawable.user_info);
        v.setBackground(drawable);
        return v;
    }
    @Override
    public boolean onNextButtonHandler() {
        return true;
    }
}
