package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChienDichGiamGiaSpecification {
    public static Specification<ChienDichGiamGia> buildFilter(
            String keyword, LocalDateTime startDate, LocalDateTime endDate,
            String status, String discountLevel
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Lọc theo từ khóa (tên hoặc mã)
            if (keyword != null && !keyword.isBlank()) {
                predicate = cb.and(predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("ten")), "%" + keyword.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("ma")), "%" + keyword.toLowerCase() + "%")
                        )
                );
            }

            // Lọc theo ngày bắt đầu
            if (startDate != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("ngayBatDau"), startDate));
            }

            // Lọc theo ngày kết thúc
            if (endDate != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("ngayKetThuc"), endDate));
            }

            // Lọc theo trạng thái chiến dịch
            if (status != null && !status.isBlank()) {
                final LocalDateTime now = LocalDateTime.now();

                switch (status) {
                    case "ONGOING" -> predicate = cb.and(predicate,
                            cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                            cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now));
                    case "UPCOMING" -> predicate = cb.and(predicate,
                            cb.greaterThan(root.get("ngayBatDau"), now));
                    case "ENDED" -> predicate = cb.and(predicate,
                            cb.lessThan(root.get("ngayKetThuc"), now));
                }
            }

            // Lọc theo mức giảm giá
            if (discountLevel != null && !discountLevel.isBlank()) {
                try {
                    BigDecimal level = new BigDecimal(discountLevel);
                    predicate = cb.and(predicate,
                            cb.greaterThanOrEqualTo(root.get("phanTramGiam"), level));
                } catch (NumberFormatException ignored) {}
            }

            return predicate;
        };
    }
}
