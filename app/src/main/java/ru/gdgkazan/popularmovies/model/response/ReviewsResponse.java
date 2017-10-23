package ru.gdgkazan.popularmovies.model.response;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import ru.gdgkazan.popularmovies.model.content.Review;

public class ReviewsResponse {

    @SerializedName("results")
    private List<Review> reviews;

    @NonNull
    public List<Review> getReviews() {
        if (reviews == null) {
            reviews = new ArrayList<>();
        }
        return reviews;
    }
}
