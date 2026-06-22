package com.example.flight.flight.favorite;

import com.example.flight.auth.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteRecord> list(@RequestAttribute("user") User user) {
        return favoriteService.list(user.id());
    }

    @PostMapping
    public ResponseEntity<FavoriteRecord> create(@RequestAttribute("user") User user,
                                                 @Valid @RequestBody CreateFavoriteRequest request) {
        FavoriteRecord favorite = favoriteService.create(user.id(), request.flightId());
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> delete(@RequestAttribute("user") User user,
                                       @PathVariable Long favoriteId) {
        favoriteService.delete(user.id(), favoriteId);
        return ResponseEntity.noContent().build();
    }

    public record CreateFavoriteRequest(@NotNull Long flightId) {
    }
}
