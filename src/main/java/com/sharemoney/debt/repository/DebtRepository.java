package com.sharemoney.debt.repository;

import com.sharemoney.debt.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    @Query("""
            select d from Debt d
            join fetch d.creditor
            join fetch d.debtor
            where d.creditor.id = :creditorId
              and (:debtorUsername is null or lower(d.debtor.username) = lower(:debtorUsername))
              and (:searchPattern is null
                   or lower(d.title) like :searchPattern
                   or lower(d.debtor.fullName) like :searchPattern
                   or lower(d.creditor.fullName) like :searchPattern)
            order by d.sortOrder asc nulls last, d.createdAt desc
            """)
    List<Debt> findAllForCreditor(@Param("creditorId") Long creditorId,
                                   @Param("debtorUsername") String debtorUsername,
                                   @Param("searchPattern") String searchPattern);

    @Query("""
            select d from Debt d
            join fetch d.creditor
            join fetch d.debtor
            where d.debtor.id = :debtorId
              and (:searchPattern is null
                   or lower(d.title) like :searchPattern
                   or lower(d.debtor.fullName) like :searchPattern
                   or lower(d.creditor.fullName) like :searchPattern)
            order by d.sortOrder asc nulls last, d.createdAt desc
            """)
    List<Debt> findAllForDebtor(@Param("debtorId") Long debtorId, @Param("searchPattern") String searchPattern);

    @Query("""
            select d from Debt d
            join fetch d.creditor
            join fetch d.debtor
            where d.id = :id
            """)
    Optional<Debt> findDetailById(@Param("id") Long id);
}
