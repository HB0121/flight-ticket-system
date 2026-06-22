package com.example.flight.flight.favorite;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public List<FavoriteRecord> list(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public FavoriteRecord create(Long userId, Long flightId) {
        return favoriteRepository.create(userId, flightId);
    }

    public void delete(Long userId, Long favoriteId) {
        favoriteRepository.deleteByUserAndFavoriteId(userId, favoriteId);
    }
}
