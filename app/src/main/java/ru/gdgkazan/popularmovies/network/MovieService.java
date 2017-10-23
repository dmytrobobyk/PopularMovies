package ru.gdgkazan.popularmovies.network;

import retrofit2.http.GET;
import retrofit2.http.Path;
import ru.gdgkazan.popularmovies.model.response.MoviesResponse;
import ru.gdgkazan.popularmovies.model.response.ReviewsResponse;
import ru.gdgkazan.popularmovies.model.response.VideosResponse;
import rx.Observable;

public interface MovieService {

    @GET("popular/")
    Observable<MoviesResponse> popularMovies();

    @GET("{videoId}/videos")
    Observable<VideosResponse> getVideo(@Path("videoId") int videoId);

    @GET("{movieId}/reviews")
    Observable<ReviewsResponse> getReviews(@Path("movieId") int movieId);
}
