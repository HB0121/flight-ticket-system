package com.example.flight.flight.favorite.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFavoriteRequest(@NotNull Long flightId) {
}
