package com.filmexa.stream.modules.myList.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.filmexa.stream.common.exception.ConflictException;
import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.myList.dto.AddToMyListRequest;
import com.filmexa.stream.modules.myList.dto.MyListItemResponse;
import com.filmexa.stream.modules.myList.entity.MyListItem;
import com.filmexa.stream.modules.myList.repo.MyListRepository;
import com.filmexa.stream.modules.myList.service.MyListService;
import com.filmexa.stream.modules.users.entity.User;

@Service
public class MyListServiceImpl implements MyListService {

    private final MyListRepository myListRepository;
    private final MovieProvider movieProvider;

    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;

    public MyListServiceImpl(MyListRepository myListRepository, MovieProvider movieProvider) {
        this.myListRepository = myListRepository;
        this.movieProvider = movieProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyListItemResponse> getMyList(User user, Pageable pageable) {
        return myListRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public MyListItemResponse addToMyList(User user, AddToMyListRequest request) {
        Long movieId = request.getMovieId();

        if (myListRepository.existsByUserIdAndMovieId(user.getId(), movieId)) {
            throw new ConflictException("Movie is already in your list");
        }

        // Throws NotFoundException when the movie does not exist upstream.
        MovieProvederData movie = movieProvider.getMovieById(request.getLanguage(), movieId.intValue());
        if (movie == null) {
            throw new NotFoundException("Movie does not exist");
        }

        MyListItem item = new MyListItem();
        item.setUser(user);
        item.setMovieId(movieId);
        item.setTitle(movie.getTitle());
        item.setPosterUrl(movie.getPoster_path() != null ? this.imageBaseUrl + movie.getPoster_path() : null);
        item.setReleaseDate(movie.getRelease_date());
        item.setRating(movie.getVote_average());
        item.setAddedAt(LocalDateTime.now());

        return toResponse(myListRepository.save(item));
    }

    @Override
    @Transactional
    public void removeFromMyList(User user, Long movieId) {
        MyListItem item = myListRepository.findByUserIdAndMovieId(user.getId(), movieId)
                .orElseThrow(() -> new NotFoundException("Movie is not in your list"));
        myListRepository.delete(item);
    }

    private MyListItemResponse toResponse(MyListItem item) {
        return new MyListItemResponse(
                item.getId(),
                item.getMovieId(),
                item.getTitle(),
                item.getPosterUrl(),
                item.getReleaseDate(),
                item.getRating(),
                item.getAddedAt()
        );
    }
}
