package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.LoanRequest;
import com.mini_project.library.dto.response.LoanResponse;
import com.mini_project.library.entity.enums.LoanStatus;
import com.mini_project.library.exception.BadRequestException;
import com.mini_project.library.exception.BookNotAvailableException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.LoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean LoanService loanService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private LoanResponse activeLoanResponse() {
        return LoanResponse.builder()
            .id(1L).bookId(1L).bookTitle("Clean Code").bookIsbn("978-0132350884")
            .userId(10L).username("john").userFullName("John Doe")
            .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(14))
            .status(LoanStatus.ACTIVE).overdue(false).daysOverdue(0)
            .fineAmount(BigDecimal.ZERO).finePaid(false)
            .build();
    }

    private LoanResponse returnedLoanResponse() {
        return LoanResponse.builder()
            .id(1L).bookId(1L).bookTitle("Clean Code").bookIsbn("978-0132350884")
            .userId(10L).username("john").userFullName("John Doe")
            .issueDate(LocalDate.now().minusDays(10)).dueDate(LocalDate.now().plusDays(4))
            .returnDate(LocalDate.now())
            .status(LoanStatus.RETURNED).overdue(false).daysOverdue(0)
            .fineAmount(BigDecimal.ZERO).finePaid(false)
            .build();
    }

    private LoanResponse finePaidLoanResponse() {
        return LoanResponse.builder()
            .id(1L).bookId(1L).bookTitle("Clean Code").bookIsbn("978-0132350884")
            .userId(10L).username("john").userFullName("John Doe")
            .issueDate(LocalDate.now().minusDays(10)).dueDate(LocalDate.now().plusDays(4))
            .returnDate(LocalDate.now())
            .status(LoanStatus.RETURNED).overdue(false).daysOverdue(0)
            .fineAmount(BigDecimal.ZERO).finePaid(true)
            .build();
    }

    @Test
    @WithMockUser(username = "librarian1", roles = "LIBRARIAN")
    @DisplayName("POST /api/loans - returns 201 when book is issued successfully")
    void issueLoan_returnsCreated_whenBookIsAvailable() throws Exception {
        LoanRequest request = new LoanRequest();
        request.setBookId(1L);
        request.setUserId(10L);

        given(loanService.issueLoan(any(LoanRequest.class), eq("librarian1")))
            .willReturn(activeLoanResponse());

        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.bookTitle").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/loans - returns 400 when bookId is null")
    void issueLoan_returns400_whenBookIdMissing() throws Exception {
        LoanRequest request = new LoanRequest();
        request.setUserId(10L);

        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.bookId").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/loans - returns 409 when book has no available copies")
    void issueLoan_returns409_whenBookUnavailable() throws Exception {
        LoanRequest request = new LoanRequest();
        request.setBookId(1L);
        request.setUserId(10L);

        given(loanService.issueLoan(any(LoanRequest.class), anyString()))
            .willThrow(new BookNotAvailableException("No available copies"));

        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("POST /api/loans - returns 403 for MEMBER role")
    void issueLoan_returns403_forMember() throws Exception {
        LoanRequest request = new LoanRequest();
        request.setBookId(1L);
        request.setUserId(10L);

        mockMvc.perform(post("/api/loans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PATCH /api/loans/{id}/return - returns 200 on successful return")
    void returnBook_returnsOk_whenLoanIsActive() throws Exception {
        given(loanService.returnBook(1L)).willReturn(returnedLoanResponse());

        mockMvc.perform(patch("/api/loans/1/return").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RETURNED"))
            .andExpect(jsonPath("$.data.fineAmount").value(0));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PATCH /api/loans/{id}/return - returns 400 when loan already returned")
    void returnBook_returns400_whenAlreadyReturned() throws Exception {
        given(loanService.returnBook(1L))
            .willThrow(new BadRequestException("Loan id=1 has already been returned"));

        mockMvc.perform(patch("/api/loans/1/return").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PATCH /api/loans/{id}/pay-fine - returns 200 when fine is paid")
    void payFine_returnsOk_whenFineIsPending() throws Exception {
        given(loanService.payFine(1L)).willReturn(finePaidLoanResponse());

        mockMvc.perform(patch("/api/loans/1/pay-fine").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.finePaid").value(true));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("PATCH /api/loans/{id}/pay-fine - returns 403 for MEMBER role")
    void payFine_returns403_forMember() throws Exception {
        mockMvc.perform(patch("/api/loans/1/pay-fine").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/loans - returns paginated list of all loans")
    void getAllLoans_returnsPage() throws Exception {
        given(loanService.getAllLoans(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(activeLoanResponse())));

        mockMvc.perform(get("/api/loans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/loans - returns 403 for MEMBER role")
    void getAllLoans_returns403_forMember() throws Exception {
        mockMvc.perform(get("/api/loans"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/loans/{id} - returns loan for any authenticated role")
    void getLoanById_returnsLoan_forMember() throws Exception {
        given(loanService.getLoanById(1L)).willReturn(activeLoanResponse());

        mockMvc.perform(get("/api/loans/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/loans/{id} - returns 404 when loan does not exist")
    void getLoanById_returns404_whenNotFound() throws Exception {
        given(loanService.getLoanById(99L))
            .willThrow(new ResourceNotFoundException("Loan", "id", 99L));

        mockMvc.perform(get("/api/loans/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/loans/user/{userId} - returns loans for a specific user")
    void getLoansByUser_returnsLoans() throws Exception {
        given(loanService.getLoansByUser(eq(10L), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(activeLoanResponse())));

        mockMvc.perform(get("/api/loans/user/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].userId").value(10));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/loans/overdue - returns paginated overdue loans")
    void getOverdueLoans_returnsPage() throws Exception {
        given(loanService.getOverdueLoans(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/loans/overdue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/loans/status/ACTIVE - returns loans filtered by status")
    void getLoansByStatus_returnsFilteredLoans() throws Exception {
        given(loanService.getLoansByStatus(eq(LoanStatus.ACTIVE), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(activeLoanResponse())));

        mockMvc.perform(get("/api/loans/status/ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/loans/sync-overdue - returns 200 and syncs overdue statuses")
    void syncOverdue_returnsOk() throws Exception {
        willDoNothing().given(loanService).syncOverdueStatuses();

        mockMvc.perform(post("/api/loans/sync-overdue").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
