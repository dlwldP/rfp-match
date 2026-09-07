package com.rfpmatch.match.repository;

import com.rfpmatch.match.domain.MatchResult;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByBidIdAndProductIdOrderByRequirementIdAsc(Long bidId, Long productId);

    void deleteByBidIdAndProductId(Long bidId, Long productId);

    void deleteByBidId(Long bidId);

    void deleteByProductId(Long productId);

    void deleteByRequirementIdIn(Collection<Long> requirementIds);

    /** 목록 화면의 매칭 여부 배지("매칭완료"/"미매칭")를 한 번에 계산한다. */
    @Query("select distinct m.bidId from MatchResult m where m.bidId in :bidIds")
    List<Long> findMatchedBidIds(@Param("bidIds") Collection<Long> bidIds);
}
