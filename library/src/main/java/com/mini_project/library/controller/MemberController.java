package com.mini_project.library.controller;

import com.mini_project.library.dto.MemberRequest;
import com.mini_project.library.entity.Member;
import com.mini_project.library.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public Member addMember(@RequestBody @Valid MemberRequest request) {
        Member member = new Member();
        member.setName(request.name());
        member.setEmail(request.email());
        return memberService.addMember(member);
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id);
    }
}