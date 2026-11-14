package com.mundovolvo.sgrv;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class BusinessRulesTest {
 @Test void calculateIgVAndDiscount() {
  BusinessRules rules=new BusinessRules(null);
  BusinessRules.Totals total=rules.calculate(List.of(Map.of("line_total",new BigDecimal("200.00"))),new BigDecimal("10"));
  assertEquals(new BigDecimal("200.00"),total.subtotal());
  assertEquals(new BigDecimal("20.00"),total.discountAmount());
  assertEquals(new BigDecimal("32.40"),total.tax());
  assertEquals(new BigDecimal("212.40"),total.total());
 }
 @Test void discountOutOfRange(){BusinessRules rules=new BusinessRules(null);assertThrows(ApiException.class,()->rules.calculate(List.of(),new BigDecimal("101")));}
}
