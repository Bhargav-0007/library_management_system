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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
        Book checkedOutBook = Book.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(2).availableCopies(1).build();
        Loan loan = Loan.builder()
            .id(100L).book(checkedOutBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(10))
            .dueDate(LocalDate.now().plusDays(4))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willReturn(loan);

        LoanResponse result = loanService.returnBook(100L);

        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(checkedOutBook.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    @DisplayName("returnBook: calculates fine when returned late")
    void returnBook_withFine() {
        Book checkedOutBook = Book.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(2).availableCopies(1).build();
        Loan loan = Loan.builder()
            .id(101L).book(checkedOutBook).user(borrower)
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

    @Test
    @DisplayName("payFine: marks fine as paid when fine is pending")
    void payFine_marksPaid_whenFineIsPending() {
        Loan loan = Loan.builder().id(100L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(20)).dueDate(LocalDate.now().minusDays(6))
            .status(LoanStatus.RETURNED).fineAmount(new BigDecimal("3.00")).finePaid(false).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));
        given(loanRepository.save(any(Loan.class))).willReturn(loan);

        loanService.payFine(100L);

        assertThat(loan.isFinePaid()).isTrue();
        then(loanRepository).should().save(loan);
    }

    @Test
    @DisplayName("payFine: throws when no fine is associated with the loan")
    void payFine_throws_whenFineIsZero() {
        Loan loan = Loan.builder().id(100L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(10)).dueDate(LocalDate.now().plusDays(4))
            .status(LoanStatus.RETURNED).fineAmount(BigDecimal.ZERO).finePaid(false).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.payFine(100L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("No fine");
    }

    @Test
    @DisplayName("payFine: throws when fine has already been paid")
    void payFine_throws_whenAlreadyPaid() {
        Loan loan = Loan.builder().id(100L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(20)).dueDate(LocalDate.now().minusDays(6))
            .status(LoanStatus.RETURNED).fineAmount(new BigDecimal("3.00")).finePaid(true).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.payFine(100L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("getLoanById: returns LoanResponse when loan exists")
    void getLoanById_returnsLoan_whenFound() {
        Loan loan = Loan.builder().id(100L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));

        LoanResponse result = loanService.getLoanById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("getLoanById: throws ResourceNotFoundException when loan does not exist")
    void getLoanById_throws_whenNotFound() {
        given(loanRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getLoanById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Loan");
    }

    @Test
    @DisplayName("getAllLoans: returns paginated list of all loans")
    void getAllLoans_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = Loan.builder().id(1L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(loan)));

        var result = loanService.getAllLoans(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("getLoansByUser: returns paginated loans for a known user")
    void getLoansByUser_returnsPage_whenUserExists() {
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = Loan.builder().id(1L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(userRepository.existsById(10L)).willReturn(true);
        given(loanRepository.findByUserId(10L, pageable)).willReturn(new PageImpl<>(List.of(loan)));

        var result = loanService.getLoansByUser(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getLoansByUser: throws ResourceNotFoundException when user does not exist")
    void getLoansByUser_throws_whenUserNotFound() {
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> loanService.getLoansByUser(99L, PageRequest.of(0, 20)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
    }

    @Test
    @DisplayName("getLoansByStatus: returns loans filtered by status")
    void getLoansByStatus_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = Loan.builder().id(1L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findByStatus(LoanStatus.ACTIVE, pageable))
            .willReturn(new PageImpl<>(List.of(loan)));

        var result = loanService.getLoansByStatus(LoanStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("getOverdueLoans: returns paginated overdue loans")
    void getOverdueLoans_returnsOverduePage() {
        Pageable pageable = PageRequest.of(0, 20);
        given(loanRepository.findOverdueLoansPage(any(LocalDate.class), eq(pageable)))
            .willReturn(new PageImpl<>(List.of()));

        var result = loanService.getOverdueLoans(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("syncOverdueStatuses: marks ACTIVE loans past due date as OVERDUE")
    void syncOverdueStatuses_marksLoansOverdue() {
        Loan activeOverdue = Loan.builder().id(1L)
            .book(availableBook).user(borrower)
            .issueDate(LocalDate.now().minusDays(20)).dueDate(LocalDate.now().minusDays(6))
            .status(LoanStatus.ACTIVE).fineAmount(BigDecimal.ZERO).build();
        given(loanRepository.findOverdueLoans(any(LocalDate.class)))
            .willReturn(List.of(activeOverdue));
        given(loanRepository.saveAll(anyList())).willReturn(List.of(activeOverdue));

        loanService.syncOverdueStatuses();

        assertThat(activeOverdue.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        then(loanRepository).should().saveAll(List.of(activeOverdue));
    }
}
