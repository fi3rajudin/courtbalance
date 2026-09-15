package com.fit.badminton.settlement; import java.math.BigDecimal; public record MemberBalance(long memberId, BigDecimal amountAdvanced, BigDecimal share, BigDecimal netBalance) {}
