package com.bteam.platform.core.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "Rating khong duoc de trong")
        @Min(value = 1, message = "Rating phai tu 1 den 5")
        @Max(value = 5, message = "Rating phai tu 1 den 5")
        Integer rating,

        @Size(max = 2000, message = "Binh luan khong duoc vuot qua 2000 ky tu")
        String comment
) {
}
