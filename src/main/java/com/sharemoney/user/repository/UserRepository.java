package com.sharemoney.user.repository;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findByRoleOrderByFullNameAsc(UserRole role);

    @Query("""
            select u from User u
            where u.role = :role and u.creditor.id = :creditorId
              and (:search is null or :search = ''
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(u.username) like lower(concat('%', :search, '%')))
            order by u.fullName
            """)
    List<User> searchDebtorsByCreditor(@Param("role") UserRole role, @Param("creditorId") Long creditorId, @Param("search") String search);

    @Query("""
            select u from User u
            where u.role = :role
              and (:search is null or :search = ''
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(u.username) like lower(concat('%', :search, '%')))
            order by u.fullName
            """)
    List<User> searchAllDebtors(@Param("role") UserRole role, @Param("search") String search);

    @Query("""
            select u.creditor.id as creditorId, count(u) as debtorCount
            from User u
            where u.role = :role
            group by u.creditor.id
            """)
    List<DebtorCountProjection> countDebtorsGroupByCreditor(@Param("role") UserRole role);
}
