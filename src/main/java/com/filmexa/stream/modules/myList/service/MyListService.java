package com.filmexa.stream.modules.myList.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.filmexa.stream.modules.myList.dto.AddToMyListRequest;
import com.filmexa.stream.modules.myList.dto.MyListItemResponse;
import com.filmexa.stream.modules.users.entity.User;

public interface MyListService {

    Page<MyListItemResponse> getMyList(User user, Pageable pageable);

    MyListItemResponse addToMyList(User user, AddToMyListRequest request);

    void removeFromMyList(User user, Long movieId);
}
