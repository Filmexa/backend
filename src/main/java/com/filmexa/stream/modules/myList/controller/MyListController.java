package com.filmexa.stream.modules.myList.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.myList.dto.AddToMyListRequest;
import com.filmexa.stream.modules.myList.dto.MyListItemResponse;
import com.filmexa.stream.modules.myList.service.MyListService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.security.ratelimit.RateLimit;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my-list")
@RequiredArgsConstructor
@Tag(name = "My List", description = "Watch later list APIs")
public class MyListController {

    private final MyListService myListService;

    @GetMapping
    public ResponseEntity<Page<MyListItemResponse>> getMyList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"));
        return ResponseEntity.ok(myListService.getMyList(currentUser(), pageRequest));
    }

    @PostMapping
    @RateLimit(limit = 30, windowSeconds = 60)
    public ResponseEntity<MyListItemResponse> addToMyList(@Valid @RequestBody AddToMyListRequest request) {
        MyListItemResponse created = myListService.addToMyList(currentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> removeFromMyList(@PathVariable Long movieId) {
        myListService.removeFromMyList(currentUser(), movieId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
