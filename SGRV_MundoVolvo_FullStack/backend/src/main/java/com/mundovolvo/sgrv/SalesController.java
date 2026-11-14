package com.mundovolvo.sgrv;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequestMapping("/api/sales") @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
public class SalesController {
 private final Db db;private final SaleService sales;
 SalesController(Db db,SaleService sales){this.db=db;this.sales=sales;}
 @GetMapping public List<Map<String,Object>> list(){return db.rows("SELECT s.*,c.name AS customer_name FROM sales s JOIN customers c ON c.id=s.customer_id ORDER BY s.id DESC LIMIT 1000");}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable long id){return sales.get(id);}
 @PostMapping public Map<String,Object> create(@RequestBody Map<String,Object> p){return sales.create(p);}
 @PostMapping("/{id}/cancel") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> cancel(@PathVariable long id){return sales.cancel(id);}
}
