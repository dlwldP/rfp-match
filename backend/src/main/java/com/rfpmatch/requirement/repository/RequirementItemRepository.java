package com.rfpmatch.requirement.repository;

import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequirementItemRepository extends JpaRepository<RequirementItem, Long> {

    List<RequirementItem> findByBidIdOrderByIdAsc(Long bidId);

    long countByBidId(Long bidId);

    void deleteByBidId(Long bidId);

    /** 목록 화면의 "요구사항 N건" 표기를 공고마다 조회하지 않고 한 번에 가져온다. */
    @Query("""
            select r.bidId as bidId, count(r) as count
            from RequirementItem r
            where r.bidId in :bidIds
            group by r.bidId
            """)
    List<BidRequirementCount> countByBidIds(@Param("bidIds") Collection<Long> bidIds);

    interface BidRequirementCount {
        Long getBidId();

        long getCount();
    }
}
