package au.gov.ga.geodesy.domain.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface ContactTypeRepository extends JpaRepository<ContactType, Integer>, QueryDslPredicateExecutor<ContactType> {

    @Override
    List<ContactType> findAll();

    ContactType findByCode(String code);

    @Query("select c from ContactType c where c.code = '" + ContactType.SITE_OWNER + "'")
    ContactType siteOwner();

    @Query("select c from ContactType c where c.code = '" + ContactType.SITE_CONTACT + "'")
    ContactType siteContact();

    @Query("select c from ContactType c where c.code = '" + ContactType.SITE_METADATA_CUSTODIAN + "'")
    ContactType siteMetadataCustodian();

    @Query("select c from ContactType c where c.code = '" + ContactType.SITE_DATA_SOURCE + "'")
    ContactType siteDataSource();

    @Query("select c from ContactType c where c.code = '" + ContactType.SITE_DATA_CENTER + "'")
    ContactType siteDataCenter();
}
