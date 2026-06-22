package com.example.flight.flight.favorite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class FavoriteServiceTest {

    @Test
    void listsFavoritesForCurrentUser() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository);
        when(favoriteRepository.findByUserId(7L)).thenReturn(List.of());

        assertThat(service.list(7L)).isEmpty();

        verify(favoriteRepository).findByUserId(7L);
    }

    @Test
    void createsFavoriteForCurrentUser() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository);

        service.create(7L, 99L);

        verify(favoriteRepository).create(7L, 99L);
    }

    @Test
    void deletesFavoriteForCurrentUser() {
        FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
        FavoriteService service = new FavoriteService(favoriteRepository);

        service.delete(7L, 11L);

        verify(favoriteRepository).deleteByUserAndFavoriteId(7L, 11L);
    }
}
