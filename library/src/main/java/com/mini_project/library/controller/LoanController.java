package com.mini_project.library.controller;

import com.mini_project.library.entity.Loan;
import com.mini_project.library.service.LoanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/borrow")
    public Loan borrow(@RequestParam Long bookId,
                       @RequestParam Long memberId) {
        return loanService.borrowBook(bookId, memberId);
    }

    @PostMapping("/return")
    public Loan returnBook(@RequestParam Long loanId) {
        return loanService.returnBook(loanId);
    }

    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    @GetMapping("/overdue")
    public java.util.List<Loan> getOverdueLoans() {
        return loanService.getOverdueLoans();
    }
}