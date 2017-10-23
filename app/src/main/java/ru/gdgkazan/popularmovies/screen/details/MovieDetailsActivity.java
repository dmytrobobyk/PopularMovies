package ru.gdgkazan.popularmovies.screen.details;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import ru.gdgkazan.popularmovies.R;
import ru.gdgkazan.popularmovies.model.content.Movie;
import ru.gdgkazan.popularmovies.model.content.Review;
import ru.gdgkazan.popularmovies.model.content.Video;
import ru.gdgkazan.popularmovies.model.response.ReviewsResponse;
import ru.gdgkazan.popularmovies.model.response.VideosResponse;
import ru.gdgkazan.popularmovies.network.ApiFactory;
import ru.gdgkazan.popularmovies.screen.loading.LoadingDialog;
import ru.gdgkazan.popularmovies.screen.loading.LoadingView;
import ru.gdgkazan.popularmovies.utils.Images;
import ru.gdgkazan.popularmovies.utils.Logger;
import ru.gdgkazan.popularmovies.utils.TextUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MovieDetailsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = Logger.getClassTag(MovieDetailsActivity.class);

    private static final String MAXIMUM_RATING = "10";

    public static final String IMAGE = "image";
    public static final String EXTRA_MOVIE = "extraMovie";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbar;

    @BindView(R.id.contentLayout)
    RelativeLayout contentLayout;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    TextView mTitleTextView;

    @BindView(R.id.overview)
    TextView mOverviewTextView;

    @BindView(R.id.rating)
    TextView mRatingTextView;

    @BindView(R.id.trailersTitle)
    TextView trailersTitle;

    @BindView(R.id.trailersLayout)
    LinearLayout trailersLayout;

    @BindView(R.id.webView)
    WebView webView;

    @BindView(R.id.reviews)
    TextView reviewsTitle;

    @BindView(R.id.reviewsLayout)
    LinearLayout reviewsLayout;

    private CompositeSubscription subscriptions;
    private LoadingView loadingView;
    private int loadingCounter;


    public static void navigate(@NonNull AppCompatActivity activity, @NonNull View transitionImage,
                                @NonNull Movie movie) {
        Intent intent = new Intent(activity, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, IMAGE);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareWindowForAnimation();
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        ViewCompat.setTransitionName(findViewById(R.id.app_bar), IMAGE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadingView = LoadingDialog.view(getSupportFragmentManager());
        subscriptions = new CompositeSubscription();

        Movie movie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        showMovie(movie);

        getVideoTrailers(movie.getId());
        getReviews(movie.getId());
    }

    private void getVideoTrailers(int movieId) {
        addSubscription(ApiFactory.getMoviesService()
                .getVideo(movieId)
                .map(VideosResponse::getVideos)
                .flatMap(videos -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.delete(Video.class);
                        realm.insert(videos);
                    });
                    return Observable.just(videos);
                }).onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Video> results = realm.where(Video.class).findAll();
                    return Observable.just(realm.copyFromRealm(results));
                }).doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(this::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccessGetMovie, this::onErrorGetMovie)
        );
    }

    private void onSuccessGetMovie(List<Video> videos) {
        if (videos.size() == 0) {
            return;
        }
        for (int i = 0; i < videos.size(); i++) {
            addTrailerTextView(videos.get(i));
        }
        showTrailer(videos.get(0));
    }

    private void onErrorGetMovie(Throwable throwable) {
        Logger.t(LOG_TAG, throwable);
    }

    private void getReviews(int movieId) {
        addSubscription(ApiFactory.getMoviesService()
                .getReviews(movieId)
                .map(ReviewsResponse::getReviews)
                .flatMap(reviews -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.delete(Review.class);
                        realm.insert(reviews);
                    });
                    return Observable.just(reviews);
                }).onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Review> results = realm.where(Review.class).findAll();
                    return Observable.just(realm.copyFromRealm(results));
                }).doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(this::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSuccessGetReviews, this::onErrorGetReviews)
        );

    }

    private void onSuccessGetReviews(List<Review> reviews) {
        if (reviews.size() == 0) {
            reviewsTitle.setVisibility(View.INVISIBLE);
        } else {
            reviewsTitle.setVisibility(View.VISIBLE);
            for (int i = 0; i < reviews.size(); i++) {
                addReviewTextView(reviews.get(i), true);
                addReviewTextView(reviews.get(i), false);
            }
        }
    }

    private void onErrorGetReviews(Throwable throwable) {
        Logger.t(LOG_TAG, throwable);
    }

    @Override
    protected void onPause() {
        if (subscriptions != null) {
            subscriptions.unsubscribe();
        }
        webView.onPause();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideLoadingIndicator() {
        loadingCounter++;
        if (loadingCounter == 2 && loadingView != null) {
            loadingView.hideLoadingIndicator();
        }

    }

    private void prepareWindowForAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    private void showMovie(@NonNull Movie movie) {
        String title = getString(R.string.movie_details);
        mCollapsingToolbar.setTitle(title);
        mCollapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, android.R.color.transparent));

        Images.loadMovie(mImage, movie, Images.WIDTH_780);

        String year = movie.getReleasedDate().substring(0, 4);
        mTitleTextView.setText(getString(R.string.movie_title, movie.getTitle(), year));
        mOverviewTextView.setText(movie.getOverview());

        String average = String.valueOf(movie.getVoteAverage());
        average = average.length() > 3 ? average.substring(0, 3) : average;
        average = average.length() == 3 && average.charAt(2) == '0' ? average.substring(0, 1) : average;
        mRatingTextView.setText(getString(R.string.rating, average, MAXIMUM_RATING));
    }

    private void showTrailer(Video video) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadData(getHTML(video.getKey()), "text/html", "utf-8");
    }

    private String getHTML(String key) {
        String html = "<iframe class=\"youtube-player\" style=\"border: 0; width: 100%; height: 95%; padding:0px; margin:0px\" id=\"ytplayer\" type=\"text/html\" src=\"http://www.youtube.com/embed/"
                + key
                + "?fs=0\" frameborder=\"0\">\n"
                + "</iframe>\n";
        return html;
    }

    private void addTrailerTextView(Video video) {
        TextView textView = new TextView(this);
        TextUtils.setSpannableString(textView, video.getName());
        textView.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        textView.setTextSize(18);
        textView.setTag(video);
        textView.setOnClickListener(this);
        trailersLayout.addView(textView);
    }

    private void addReviewTextView(Review review, boolean header) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 0);
        if (header) {
            textView.setText(review.getAuthor());
            textView.setTextSize(16);
            textView.setTextColor(ContextCompat.getColor(
                    textView.getContext(), R.color.textColorPrimary));
        } else {
            textView.setTextSize(14);
            textView.setTextColor(ContextCompat.getColor(
                    textView.getContext(), R.color.textColorSecondary));
            textView.setAutoLinkMask(Linkify.WEB_URLS);
            textView.setLinkTextColor(ContextCompat.getColor(
                    textView.getContext(), R.color.colorPrimary));
            textView.setText(review.getContent());
        }
        textView.setLayoutParams(params);
        reviewsLayout.addView(textView);
    }


    private void addSubscription(@NonNull Subscription s) {
        if (subscriptions != null) {
            subscriptions.add(s);
        }
    }

    @Override
    public void onClick(View v) {
        Video video = (Video) v.getTag();
        showTrailer(video);
    }
}
