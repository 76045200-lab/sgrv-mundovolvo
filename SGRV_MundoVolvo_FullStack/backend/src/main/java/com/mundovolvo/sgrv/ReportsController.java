package com.mundovolvo.sgrv;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api")
public class ReportsController {
 private final Db db;
 ReportsController(Db db){this.db=db;}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){
  Map<String,Object> r=new LinkedHashMap<>();
  r.put("products",db.id("SELECT count(*) FROM products WHERE active=true"));
  r.put("lowStock",db.id("SELECT count(*) FROM products WHERE active=true AND stock<=min_stock"));
  boolean warehouse="WAREHOUSE".equals(AuthService.actor().get("role"));
  r.put("quotesThisMonth",warehouse?0L:db.id("SELECT count(*) FROM quotes WHERE date_trunc('month',created_at)=date_trunc('month',now())"));
  r.put("salesThisMonth",warehouse?java.math.BigDecimal.ZERO:db.num("SELECT coalesce(sum(total),0) FROM sales WHERE status='CONFIRMED' AND date_trunc('month',created_at)=date_trunc('month',now())"));
  r.put("recentQuotes",warehouse?List.of():db.rows("SELECT q.id,q.quote_number,q.status,q.total,q.created_at,c.name AS customer_name FROM quotes q JOIN customers c ON c.id=q.customer_id ORDER BY q.id DESC LIMIT 8"));
  return r;
 }
 @GetMapping("/reports/sales") @PreAuthorize("hasAnyRole('ADMIN','SELLER')") public List<Map<String,Object>> sales(){return db.rows("SELECT date_trunc('month',created_at)::date AS month, count(*) AS operations,coalesce(sum(total),0) AS total FROM sales WHERE status='CONFIRMED' GROUP BY 1 ORDER BY 1 DESC LIMIT 24");}
 @GetMapping("/reports/purchases") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> purchases(){return db.rows("SELECT date_trunc('month',created_at)::date AS month,count(*) AS operations,coalesce(sum(total),0) AS total FROM purchases GROUP BY 1 ORDER BY 1 DESC LIMIT 24");}
 @GetMapping("/reports/inventory") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") public List<Map<String,Object>> inventory(){return db.rows("SELECT p.id,p.code,p.name,p.stock,p.min_stock,p.sale_price,p.purchase_cost,b.name AS brand,c.name AS category FROM products p JOIN brands b ON b.id=p.brand_id JOIN categories c ON c.id=p.category_id ORDER BY p.name");}
 @GetMapping("/reports/abc") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> abc(){return db.rows("WITH item_sales AS (SELECT si.product_id,sum(si.line_total) AS revenue,sum(si.quantity) AS units FROM sale_items si JOIN sales s ON s.id=si.sale_id WHERE s.status='CONFIRMED' GROUP BY si.product_id), ranked AS (SELECT p.id,p.code,p.name,coalesce(i.revenue,0) AS revenue,coalesce(i.units,0) AS units,p.stock,sum(coalesce(i.revenue,0)) OVER(ORDER BY coalesce(i.revenue,0) DESC,p.id) AS cumulative,sum(coalesce(i.revenue,0)) OVER() AS grand FROM products p LEFT JOIN item_sales i ON i.product_id=p.id) SELECT id,code,name,revenue,units,stock,CASE WHEN grand=0 THEN 'SIN_DATOS' WHEN (cumulative-revenue)/grand<0.80 THEN 'A' WHEN (cumulative-revenue)/grand<0.95 THEN 'B' ELSE 'C' END AS abc FROM ranked ORDER BY revenue DESC,id LIMIT 5000");}
 @GetMapping("/reports/rotation") @PreAuthorize("hasRole('ADMIN')")
 public List<Map<String,Object>> rotation(@RequestParam(defaultValue="90") int days){
  if(days<1||days>366)throw ApiException.bad("Período válido: 1 a 366 días");
  return db.rows("WITH movement AS (SELECT product_id, coalesce(sum(CASE WHEN direction='IN' THEN quantity ELSE -quantity END),0) AS net FROM stock_movements WHERE created_at >= now() - (? * interval '1 day') GROUP BY product_id), sold AS (SELECT si.product_id,sum(si.quantity) AS sold_qty FROM sale_items si JOIN sales s ON s.id=si.sale_id WHERE s.status='CONFIRMED' AND s.created_at >= now() - (? * interval '1 day') GROUP BY si.product_id) SELECT p.id,p.code,p.name,p.stock,coalesce(s.sold_qty,0) AS sold_qty,p.stock-coalesce(m.net,0) AS opening_stock,round(((p.stock-coalesce(m.net,0))+p.stock)/2,3) AS average_stock,CASE WHEN ((p.stock-coalesce(m.net,0))+p.stock)>0 THEN round(coalesce(s.sold_qty,0)/(((p.stock-coalesce(m.net,0))+p.stock)/2),3) ELSE NULL END AS turnover FROM products p LEFT JOIN movement m ON m.product_id=p.id LEFT JOIN sold s ON s.product_id=p.id ORDER BY sold_qty DESC,p.id LIMIT 5000",days,days);
 }
 @GetMapping("/settings") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> settings(){return db.rows("SELECT * FROM app_settings ORDER BY key");}
 @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> audit(){return db.rows("SELECT a.*,u.username AS actor FROM audit_logs a LEFT JOIN app_users u ON u.id=a.user_id ORDER BY a.id DESC LIMIT 250");}
}
