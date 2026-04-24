package com.mini_project.library.service;

import com.mini_project.library.entity.Book;
import com.mini_project.library.entity.Loan;
import com.mini_project.library.entity.Member;
import com.mini_project.library.repository.BookRepository;
import com.mini_project.library.repository.LoanRepository;
import com.mini_project.library.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    public Loan borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        loanRepository.findByBookIdAndReturnDateIsNull(bookId)
                .ifPresent(existingLoan -> {
                    throw new RuntimeException("Book is already borrowed");
                });

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setMember(member);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setReturnDate(null);

        return loanRepository.save(loan);
    }

    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getReturnDate() != null) {
            throw new RuntimeException("Book already returned");
        }

        loan.setReturnDate(LocalDate.now());
        return loanRepository.save(loan);
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Loan> getOverdueLoans() {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getReturnDate() == null)
                .filter(loan -> loan.getDueDate().isBefore(LocalDate.now()))
                .toList();
    }
}