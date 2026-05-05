package com.mini_project.library.service;

import com.mini_project.library.dto.request.LoanRequest;
import com.mini_project.library.dto.response.LoanResponse;
import com.mini_project.library.entity.Book;
import com.mini_project.library.entity.Loan;
import com.mini_project.library.entity.User;
import com.mini_project.library.entity.enums.LoanStatus;
import com.mini_project.library.exception.BadRequestException;
import com.mini_project.library.exception.BookNotAvailableException;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock BookService bookService;
    @Mock UserRepository userRepository;

    @InjectMocks LoanService loanService;

    private Book availableBook;
    private Book unavailableBook;
    private User borrower;
    private LoanRequest loanRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loanService, "loanDurationDays", 14);
        ReflectionTestUtils.setField(loanService, "finePerDay", new BigDecimal("0.50"));

        availableBook = Book.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(2).availableCopies(2).build();

        unavailableBook = Book.builder()
            .id(2L).title("Out of Stock").isbn("000-0000000000")
            .totalCopies(1).availableCopies(0).build();

        borrower = User.builder()
            .id(10L).username("john").email("john@example.com").build();

        loanRequest = new LoanRequest();
        loanRequest.setBookId(1L);
        loanRequest.setUserId(10L);
    }

    @Test
    @DisplayName("issueLoan: creates loan when book is available")
    void issueLoan_success() {
        given(bookService.findById(1L)).willReturn(availableBook);
        given(userRepository.findById(10L)).willReturn(Optional.of(borrower));
        given(userRepository.findByUsername("librarian1")).willReturn(Optional.empty());
        given(loanRepository.existsByUserIdAndBookIdAndStatus(10L, 1L, LoanStatus.ACTIVE)).willReturn(false);

        Loan savedLoan = Loan.builder()
            .id(100L).book(availableBook).user(borrower)
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.save(any(Loan.class))).willReturn(savedLoan);

        LoanResponse result = loanService.issueLoan(loanRequest, "librarian1");

        assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(availableBook.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    @DisplayName("issueLoan: throws when no copies available")
    void issueLoan_bookUnavailable() {
        given(bookService.findById(2L)).willReturn(unavailableBook);
        given(userRepository.findById(10L)).willReturn(Optional.of(borrower));
        loanRequest.setBookId(2L);

        assertThatThrownBy(() -> loanService.issueLoan(loanRequest, "librarian1"))
            .isInstanceOf(BookNotAvailableException.class);
    }

    @Test
    @DisplayName("issueLoan: throws when user already has active loan for same book")
    void issueLoan_duplicateLoan() {
        given(bookService.findById(1L)).willReturn(availableBook);
        given(userRepository.findById(10L)).willReturn(Optional.of(borrower));
        given(loanRepository.existsByUserIdAndBookIdAndStatus(10L, 1L, LoanStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> loanService.issueLoan(loanRequest, "librarian1"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already has an active loan");
    }

    @Test
    @DisplayName("returnBook: calculates zero fine when returned on time")
    void returnBook_noFine() {
        Loan loan = Loan.builder()
            .id(100L).book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(10))
            .dueDate(LocalDate.now().plusDays(4))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willReturn(loan);

        LoanResponse result = loanService.returnBook(100L);

        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    @DisplayName("returnBook: calculates fine when returned late")
    void returnBook_withFine() {
        Loan loan = Loan.builder()
            .id(101L).book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(20))
            .dueDate(LocalDate.now().minusDays(6))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findById(101L)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willReturn(loan);

        loanService.returnBook(101L);

        // 6 days overdue × $0.50 = $3.00
        assertThat(loan.getFineAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("returnBook: throws when loan already returned")
    void returnBook_alreadyReturned() {
        Loan loan = Loan.builder().id(102L)
            .status(LoanStatus.RETURNED).fineAmount(BigDecimal.ZERO)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(5)).dueDate(LocalDate.now().plusDays(9)).build();
        given(loanRepository.findById(102L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnBook(102L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already been returned");
    }
}
