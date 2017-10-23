package ru.gdgkazan.popularmovies.utils;

import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

/**
 * Created by dima on 24.09.17.
 */

public class TextUtils {

    public static void setSpannableString(TextView textView, String text) {
        SpannableString content = new SpannableString(text);
        content.setSpan(new UnderlineSpan(), 0, text.length(), 0);
        textView.setText(content);
    }
}
