package com.sharemoney.document.repository;

import com.sharemoney.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDebtor_Id(Long debtorId);

    @Query("""
            select d from Document d
            left join fetch d.ownerCreditor
            left join fetch d.debtor
            where d.ownerCreditor is null or d.ownerCreditor.id = :creditorId
            order by d.uploadedAt desc
            """)
    List<Document> findVisibleToCreditor(@Param("creditorId") Long creditorId);

    @Query("""
            select d from Document d
            left join fetch d.ownerCreditor
            left join fetch d.debtor
            where d.ownerCreditor is null
               or d.debtor.id = :debtorId
               or (d.debtor is null and d.ownerCreditor.id = :creditorId)
            order by d.uploadedAt desc
            """)
    List<Document> findVisibleToDebtor(@Param("debtorId") Long debtorId, @Param("creditorId") Long creditorId);

    @Query("""
            select d from Document d
            left join fetch d.ownerCreditor
            left join fetch d.debtor
            order by d.uploadedAt desc
            """)
    List<Document> findAllWithOwners();
}
