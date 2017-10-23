package ru.gdgkazan.popularmovies.model.response;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import ru.gdgkazan.popularmovies.model.content.Movie;

public class MoviesResponse {

    @SerializedName("results")
    private List<Movie> movies;

    @NonNull
    public List<Movie> getMovies() {
        if (movies == null) {
            return new ArrayList<>();
        }
        return movies;
    }

}
