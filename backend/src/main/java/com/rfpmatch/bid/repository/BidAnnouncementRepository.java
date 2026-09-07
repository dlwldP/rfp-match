package com.rfpmatch.bid.repository;

import com.rfpmatch.bid.domain.BidAnnouncement;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidAnnouncementRepository extends JpaRepository<BidAnnouncement, Long> {

    Optional<BidAnnouncement> findByBidNtceNo(String bidNtceNo);

    boolean existsByBidNtceNo(String bidNtceNo);

    /**
     * 공고명 키워드와 진행 상태로 목록을 조회한다.
     *
     * @param status OPEN(마감 전) / CLOSED(마감) / null(전체)
     */
    @Query("""
            select b from BidAnnouncement b
            where (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%')))
              and (
                    :status is null
                 or (:status = 'OPEN' and b.closeDate is not null and b.closeDate > :now)
                 or (:status = 'CLOSED' and b.closeDate is not null and b.closeDate <= :now)
                 or (:status = 'UNKNOWN' and b.closeDate is null)
              )
            order by b.closeDate desc nulls last, b.id desc
            """)
    Page<BidAnnouncement> search(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("now") LocalDateTime now,
                                 Pageable pageable);
}
