package com.mini_project.library.repository;

import com.mini_project.library.entity.Loan;
import com.mini_project.library.entity.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByUserId(Long userId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    Optional<Loan> findByBookIdAndStatus(Long bookId, LoanStatus status);

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :today")
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :today")
    Page<Loan> findOverdueLoansPage(@Param("today") LocalDate today, Pageable pageable);

    long countByUserId(Long userId);

    long countByStatus(LoanStatus status);
}
