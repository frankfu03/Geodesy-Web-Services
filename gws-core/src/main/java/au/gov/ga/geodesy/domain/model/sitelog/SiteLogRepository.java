package au.gov.ga.geodesy.domain.model.sitelog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

public interface SiteLogRepository extends
        JpaRepository<SiteLog, Integer>,
        SiteLogRepositoryCustom,
        QueryDslPredicateExecutor<SiteLog> {

    @Override
    List<SiteLog> findAll();
    
    @Query("select siteLog from SiteLog siteLog where siteLog.siteIdentification.fourCharacterId = :id")
    SiteLog findByFourCharacterId(@Param("id") String id);
}

