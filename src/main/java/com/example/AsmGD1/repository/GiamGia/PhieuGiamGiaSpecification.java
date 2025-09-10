package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuGiamGiaSpecification {

    /**
     * @param search   tìm theo mã/ tên (like, không phân biệt hoa thường)
     * @param type     lọc theo loai: PERCENT, CASH, FREESHIP_FULL, FREESHIP_CAP (case-insensitive)
     * @param status   upcoming | active | expired
     * @param fromDate lọc các voucher có giao nhau và kết thúc >= fromDate
     * @param toDate   lọc các voucher có giao nhau và bắt đầu <= toDate
     */
    public static Specification<PhieuGiamGia> filter(
            String search,
            String type,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Search theo mã/ tên ---
            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.trim().toLowerCase() + "%";
                Predicate byTen = cb.like(cb.lower(root.get("ten")), likeSearch);
                Predicate byMa  = cb.like(cb.lower(root.get("ma")), likeSearch);
                predicates.add(cb.or(byTen, byMa));
            }

            // --- Lọc theo kiểu (loai) ---
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("loai")), type.trim().toUpperCase()));
            }

            // --- Lọc theo khoảng ngày (có giao nhau) ---
            if (fromDate != null) {
                // voucher kết thúc sau hoặc đúng fromDate
                predicates.add(cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), fromDate));
            }
            if (toDate != null) {
                // voucher bắt đầu trước hoặc đúng toDate
                predicates.add(cb.lessThanOrEqualTo(root.get("ngayBatDau"), toDate));
            }

            // --- Lọc theo trạng thái ---
            if (status != null && !status.trim().isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (status) {
                    case "upcoming" -> {
                        predicates.add(cb.greaterThan(root.get("ngayBatDau"), now));
                    }
                    case "active" -> {
                        Predicate inTime = cb.and(
                                cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                                cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now)
                        );
                        Predicate qtyOk = cb.or(
                                cb.isNull(root.get("soLuong")),
                                cb.greaterThan(root.get("soLuong"), 0)
                        );
                        predicates.add(cb.and(inTime, qtyOk));
                    }
                    case "expired" -> {
                        Predicate outOfTime = cb.lessThan(root.get("ngayKetThuc"), now);
                        Predicate qtyOver   = cb.and(
                                cb.isNotNull(root.get("soLuong")),
                                cb.lessThanOrEqualTo(root.get("soLuong"), 0)
                        );
                        predicates.add(cb.or(outOfTime, qtyOver));
                    }
                    default -> {
                        // Không lọc nếu status khác 3 giá trị trên
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
