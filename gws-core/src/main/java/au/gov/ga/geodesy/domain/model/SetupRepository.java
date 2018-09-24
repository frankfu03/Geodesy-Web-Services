package au.gov.ga.geodesy.domain.model;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import au.gov.ga.geodesy.support.spring.AggregateRepository;

public interface SetupRepository extends AggregateRepository<Setup>, SetupRepositoryCustom, QueryDslPredicateExecutor<Setup> {

    // TODO: test
    @Query("select s from Setup s where s.siteId = :siteId and s.type = :type and s.invalidated = false")
    Page<Setup> findBySiteId(@Param("siteId") Integer id, @Param("type") SetupType type, Pageable pageRequest);

    @RestResource(exported = false)
    @Query("select s from Setup s where s.siteId = :siteId and s.type = :type and s.invalidated = false")
    List<Setup> findBySiteId(@Param("siteId") Integer id, @Param("type") SetupType type);

    @RestResource(exported = false)
    @Query("select s from Setup s where s.siteId = :siteId and s.type = :type and s.invalidated = true")
    List<Setup> findInvalidatedBySiteId(@Param("siteId") Integer id, @Param("type") SetupType type) ;
}
