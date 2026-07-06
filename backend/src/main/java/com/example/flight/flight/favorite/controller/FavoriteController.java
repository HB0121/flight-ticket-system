package com.example.flight.flight.favorite.controller;

import com.example.flight.auth.model.User;
import com.example.flight.flight.favorite.dto.CreateFavoriteRequest;
import com.example.flight.flight.favorite.model.FavoriteRecord;
import com.example.flight.flight.favorite.repository.FavoriteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/favorites")
@Validated
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;

    public FavoriteController(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping
    public List<FavoriteRecord> list(@RequestAttribute("user") User user) {
        return favoriteRepository.findByUserId(user.id());
    }

    @PostMapping
    public ResponseEntity<FavoriteRecord> create(@RequestAttribute("user") User user,
                                                 @Valid @RequestBody CreateFavoriteRequest request) {
        // The repository enforces "one user + one flight = one favorite".
        FavoriteRecord favorite = favoriteRepository.create(user.id(), request.flightId());
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> delete(@RequestAttribute("user") User user,
                                       @PathVariable Long favoriteId) {
        favoriteRepository.deleteByUserAndFavoriteId(user.id(), favoriteId);
        return ResponseEntity.noContent().build();
    }
}
