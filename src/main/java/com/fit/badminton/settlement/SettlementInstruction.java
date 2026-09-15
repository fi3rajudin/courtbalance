package com.fit.badminton.settlement; import java.math.BigDecimal; public record SettlementInstruction(long payerMemberId,long payeeMemberId,BigDecimal amount) {}
