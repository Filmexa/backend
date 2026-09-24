package com.filmexa.stream.modules.myList.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.common.exception.ConflictException;
import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.myList.dto.AddToMyListRequest;
import com.filmexa.stream.modules.myList.dto.MyListItemResponse;
import com.filmexa.stream.modules.myList.entity.MyListItem;
import com.filmexa.stream.modules.myList.repo.MyListRepository;
import com.filmexa.stream.modules.users.entity.User;

@ExtendWith(MockitoExtension.class)
class MyListServiceImplTest {

    private static final String IMAGE_BASE_URL = "https://images.test/";

    @Mock
    private MyListRepository myListRepository;

    @Mock
    private MovieProvider movieProvider;

    private MyListServiceImpl myListService;

    @BeforeEach
    void setUp() {
        myListService = new MyListServiceImpl(myListRepository, movieProvider);
        ReflectionTestUtils.setField(myListService, "imageBaseUrl", IMAGE_BASE_URL);
        lenient().when(myListRepository.save(any(MyListItem.class))).thenAnswer(invocation -> {
            MyListItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(UUID.randomUUID());
            }
            return item;
        });
    }

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("watcher");
        return user;
    }

    private MovieProvederData providerMovie() {
        MovieProvederData movie = new MovieProvederData();
        movie.setId(550);
        movie.setTitle("Fight Club");
        movie.setRelease_date("1999-10-15");
        movie.setVote_average(8.4);
        movie.setPoster_path("/poster.jpg");
        return movie;
    }

    private AddToMyListRequest request(long movieId) {
        AddToMyListRequest request = new AddToMyListRequest();
        request.setMovieId(movieId);
        return request;
    }

    @Test
    void addToMyList_shouldSnapshotMovieDetails() {
        User user = newUser();
        when(myListRepository.existsByUserIdAndMovieId(user.getId(), 550L)).thenReturn(false);
        when(movieProvider.getMovieById("en", 550)).thenReturn(providerMovie());

        MyListItemResponse response = myListService.addToMyList(user, request(550L));

        assertThat(response.getMovieId()).isEqualTo(550L);
        assertThat(response.getTitle()).isEqualTo("Fight Club");
        assertThat(response.getPosterUrl()).isEqualTo(IMAGE_BASE_URL + "/poster.jpg");
        assertThat(response.getReleaseDate()).isEqualTo("1999-10-15");
        assertThat(response.getRating()).isEqualTo(8.4);
        assertThat(response.getAddedAt()).isNotNull();
    }

    @Test
    void addToMyList_shouldLeavePosterNull_whenProviderHasNoPoster() {
        User user = newUser();
        MovieProvederData movie = providerMovie();
        movie.setPoster_path(null);
        when(myListRepository.existsByUserIdAndMovieId(user.getId(), 550L)).thenReturn(false);
        when(movieProvider.getMovieById("en", 550)).thenReturn(movie);

        assertThat(myListService.addToMyList(user, request(550L)).getPosterUrl()).isNull();
    }

    @Test
    void addToMyList_shouldThrowConflict_whenMovieAlreadyInList() {
        User user = newUser();
        when(myListRepository.existsByUserIdAndMovieId(user.getId(), 550L)).thenReturn(true);

        assertThatThrownBy(() -> myListService.addToMyList(user, request(550L)))
                .isInstanceOf(ConflictException.class);

        verify(movieProvider, never()).getMovieById(anyString(), anyInt());
        verify(myListRepository, never()).save(any(MyListItem.class));
    }

    @Test
    void removeFromMyList_shouldDeleteOwnItem() {
        User user = newUser();
        MyListItem item = new MyListItem();
        item.setId(UUID.randomUUID());
        item.setUser(user);
        item.setMovieId(550L);
        when(myListRepository.findByUserIdAndMovieId(user.getId(), 550L)).thenReturn(Optional.of(item));

        myListService.removeFromMyList(user, 550L);

        verify(myListRepository).delete(item);
    }

    @Test
    void removeFromMyList_shouldThrowNotFound_whenMovieNotInList() {
        User user = newUser();
        when(myListRepository.findByUserIdAndMovieId(user.getId(), 550L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myListService.removeFromMyList(user, 550L))
                .isInstanceOf(NotFoundException.class);

        verify(myListRepository, never()).delete(any(MyListItem.class));
    }

    @Test
    void getMyList_shouldMapStoredItems() {
        User user = newUser();
        MyListItem item = new MyListItem();
        item.setId(UUID.randomUUID());
        item.setUser(user);
        item.setMovieId(550L);
        item.setTitle("Fight Club");
        item.setPosterUrl(IMAGE_BASE_URL + "/poster.jpg");
        item.setAddedAt(LocalDateTime.now());
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(myListRepository.findByUserId(user.getId(), pageRequest))
                .thenReturn(new PageImpl<>(List.of(item), pageRequest, 1));

        Page<MyListItemResponse> page = myListService.getMyList(user, pageRequest);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Fight Club");
    }
}
