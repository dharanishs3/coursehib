package com.coursehub.helper;

import com.coursehub.repository.CourseReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Average rating and number of ratings of a course, calculated from the review records. */
@Component
@RequiredArgsConstructor
public class RatingHelper {

    private final CourseReviewRepository courseReviewRepository;

    // Rounded to 2 decimals. A course without reviews has 0.00 (never null).
    public BigDecimal getAverageRating(Long courseId) {
        Double average = courseReviewRepository.findAverageRatingByCourseId(courseId);
        if (average == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getTotalRatings(Long courseId) {
        return courseReviewRepository.countByCourseId(courseId);
    }
}
