package com.fit.badminton.settlement; import java.math.BigDecimal; import java.util.*;
public record SettlementPreview(BigDecimal totalCost,Map<Long,BigDecimal> shares,List<MemberBalance> balances,List<SettlementInstruction> transfers){}
