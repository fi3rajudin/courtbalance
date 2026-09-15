package com.fit.badminton.member.dto; import jakarta.validation.constraints.NotBlank;
public record MemberRequest(@NotBlank String name,String nickname,String paymentNote,boolean active){}
