package com.mini_project.library.service;

import com.mini_project.library.dto.request.LoanRequest;
import com.mini_project.library.dto.response.LoanResponse;
import com.mini_project.library.entity.Book;
import com.mini_project.library.entity.Loan;
import com.mini_project.library.entity.User;
import com.mini_project.library.entity.enums.LoanStatus;
import com.mini_project.library.exception.BadRequestException;
import com.mini_project.library.exception.BookNotAvailableException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final UserRepository userRepository;

    @Value("${app.loan.duration-days:14}")
    private int loanDurationDays;

    @Value("${app.loan.fine-per-day:0.50}")
    private BigDecimal finePerDay;

    @Transactional
    public LoanResponse issueLoan(LoanRequest request, String librarianUsername) {
        Book book = bookService.findById(request.getBookId());
        User borrower = findUserById(request.getUserId());

        if (!book.isAvailable()) {
            throw new BookNotAvailableException(
                "Book '" + book.getTitle() + "' has no available copies");
        }
        if (loanRepository.existsByUserIdAndBookIdAndStatus(
                borrower.getId(), book.getId(), LoanStatus.ACTIVE)) {
            throw new BadRequestException(
                "User '" + borrower.getUsername() + "' already has an active loan for this book");
        }

        User librarian = userRepository.findByUsername(librarianUsername).orElse(null);
        book.decrementAvailableCopies();

        Loan loan = Loan.builder()
            .book(book)
            .user(borrower)
            .issuedBy(librarian)
            .issueDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(loanDurationDays))
            .status(LoanStatus.ACTIVE)
            .build();

        Loan saved = loanRepository.save(loan);
        log.info("Issued loan id={} for book '{}' to user '{}'",
            saved.getId(), book.getTitle(), borrower.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = findLoanById(loanId);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BadRequestException("Loan id=" + loanId + " has already been returned");
        }
        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setFineAmount(calculateFine(loan));
        loan.getBook().incrementAvailableCopies();
        loanRepository.save(loan);
        log.info("Returned loan id={}, fine={}", loanId, loan.getFineAmount());
        return toResponse(loan);
    }

    @Transactional
    public LoanResponse payFine(Long loanId) {
        Loan loan = findLoanById(loanId);
        if (loan.getFineAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("No fine associated with loan id=" + loanId);
        }
        if (loan.isFinePaid()) {
            throw new BadRequestException("Fine for loan id=" + loanId + " already paid");
        }
        loan.setFinePaid(true);
        return toResponse(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long id) {
        return toResponse(findLoanById(id));
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoansByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return loanRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoansByStatus(LoanStatus status, Pageable pageable) {
        return loanRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getOverdueLoans(Pageable pageable) {
        return loanRepository.findOverdueLoansPage(LocalDate.now(), pageable).map(this::toResponse);
    }

    @Transactional
    public void syncOverdueStatuses() {
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        overdueLoans.forEach(loan -> loan.setStatus(LoanStatus.OVERDUE));
        loanRepository.saveAll(overdueLoans);
        log.info("Marked {} loans as OVERDUE", overdueLoans.size());
    }

    private BigDecimal calculateFine(Loan loan) {
        if (!loan.getReturnDate().isAfter(loan.getDueDate())) {
            return BigDecimal.ZERO;
        }
        long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
        return finePerDay.multiply(BigDecimal.valueOf(daysLate));
    }

    private Loan findLoanById(Long id) {
        return loanRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public LoanResponse toResponse(Loan loan) {
        long daysOverdue = 0;
        LocalDate reference = loan.getReturnDate() != null ? loan.getReturnDate() : LocalDate.now();
        if (reference.isAfter(loan.getDueDate())) {
            daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), reference);
        }
        boolean overdue = daysOverdue > 0 && loan.getStatus() != LoanStatus.RETURNED
            || (loan.getReturnDate() != null && loan.getReturnDate().isAfter(loan.getDueDate()));

        return LoanResponse.builder()
            .id(loan.getId())
            .bookId(loan.getBook().getId())
            .bookTitle(loan.getBook().getTitle())
            .bookIsbn(loan.getBook().getIsbn())
            .userId(loan.getUser().getId())
            .username(loan.getUser().getUsername())
            .userFullName(loan.getUser().getFullName())
            .issueDate(loan.getIssueDate())
            .dueDate(loan.getDueDate())
            .returnDate(loan.getReturnDate())
            .status(loan.getStatus())
            .overdue(overdue)
            .daysOverdue(daysOverdue)
            .fineAmount(loan.getFineAmount())
            .finePaid(loan.isFinePaid())
            .createdAt(loan.getCreatedAt())
            .build();
    }
}
