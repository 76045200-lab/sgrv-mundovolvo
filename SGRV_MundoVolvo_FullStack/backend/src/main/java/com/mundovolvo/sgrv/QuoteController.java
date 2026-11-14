package com.mundovolvo.sgrv;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api/quotes") @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
public class QuoteController {
 private final Db db;private final BusinessRules rules;private final SaleService sales;
 QuoteController(Db db,BusinessRules rules,SaleService sales){this.db=db;this.rules=rules;this.sales=sales;}
 @GetMapping public List<Map<String,Object>> list(){return db.rows("SELECT q.*,c.name AS customer_name FROM quotes q JOIN customers c ON c.id=q.customer_id ORDER BY q.id DESC LIMIT 1000");}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable long id){Map<String,Object> r=new LinkedHashMap<>(db.one("SELECT q.*,c.name AS customer_name,c.document AS customer_document,c.address AS customer_address FROM quotes q JOIN customers c ON c.id=q.customer_id WHERE q.id=?",id));r.put("items",db.rows("SELECT * FROM quote_items WHERE quote_id=? ORDER BY id",id));return r;}
 @PostMapping @Transactional public Map<String,Object> create(@RequestBody Map<String,Object> p){
  verifyCustomer(Db.longVal(p.get("customerId")));
  long id=db.id("INSERT INTO quotes(customer_id,discount_pct,notes,created_by) VALUES(?,?,?,?) RETURNING id",Db.longVal(p.get("customerId")),discount(p),Db.nullable(p.get("notes")),AuthService.uid());
  saveLines(id,p);db.exec("UPDATE quotes SET quote_number='COT-'||lpad(id::text,6,'0') WHERE id=?",id);
  db.audit(AuthService.uid(),"CREATE","QUOTE",id,"");return get(id);
 }
 @PutMapping("/{id}") @Transactional public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> p){
  Map<String,Object> old=db.one("SELECT status FROM quotes WHERE id=? FOR UPDATE",id);
  if(!"DRAFT".equals(old.get("status")))throw ApiException.conflict("Solo se modifican cotizaciones en borrador");
  verifyCustomer(Db.longVal(p.get("customerId")));
  db.exec("UPDATE quotes SET customer_id=?,discount_pct=?,notes=?,updated_at=now() WHERE id=?",Db.longVal(p.get("customerId")),discount(p),Db.nullable(p.get("notes")),id);
  db.exec("DELETE FROM quote_items WHERE quote_id=?",id);saveLines(id,p);db.audit(AuthService.uid(),"UPDATE","QUOTE",id,"");return get(id);
 }
 private void verifyCustomer(long id){Map<String,Object> c=db.one("SELECT active FROM customers WHERE id=?",id);if(!Boolean.TRUE.equals(c.get("active")))throw ApiException.bad("Cliente inactivo");}
 private BigDecimal discount(Map<String,Object> p){BigDecimal pct=Db.decimal(p.getOrDefault("discountPct",0),"Descuento");if(pct.signum()<0||pct.compareTo(new BigDecimal("100"))>0)throw ApiException.bad("Descuento 0 a 100 %");return pct;}
 private void saveLines(long id,Map<String,Object> p){
  List<Map<String,Object>> lines=rules.lines(p.get("items"),true);
  BigDecimal pct=discount(p);BusinessRules.Totals t=rules.calculate(lines,pct);
  for(Map<String,Object> x:lines)db.exec("INSERT INTO quote_items(quote_id,product_id,product_code,product_name,quantity,unit_price,line_total) VALUES(?,?,?,?,?,?,?)",id,x.get("product_id"),x.get("product_code"),x.get("product_name"),x.get("quantity"),x.get("unit_price"),x.get("line_total"));
  db.exec("UPDATE quotes SET subtotal=?,discount_amount=?,tax_rate=?,tax=?,total=? WHERE id=?",t.subtotal(),t.discountAmount(),BusinessRules.IGV,t.tax(),t.total(),id);
 }
 @PatchMapping("/{id}/status") @Transactional public Map<String,Object> status(@PathVariable long id,@RequestBody Map<String,Object> p){
  Map<String,Object> old=db.one("SELECT status FROM quotes WHERE id=? FOR UPDATE",id);
  String current=old.get("status").toString(),next=Db.text(p.get("status"),"Estado",20);
  boolean allowed=(current.equals("DRAFT")&&next.equals("SENT"))||(current.equals("SENT")&&(next.equals("APPROVED")||next.equals("REJECTED")));
  if(!allowed)throw ApiException.conflict("Cambio de estado no permitido: "+current+" → "+next);
  db.exec("UPDATE quotes SET status=?,updated_at=now() WHERE id=?",next,id);db.audit(AuthService.uid(),"STATUS","QUOTE",id,next);return get(id);
 }
 @DeleteMapping("/{id}") @Transactional public void delete(@PathVariable long id){
  Map<String,Object> old=db.one("SELECT status FROM quotes WHERE id=? FOR UPDATE",id);
  if(!"DRAFT".equals(old.get("status")))throw ApiException.conflict("Solo se eliminan borradores");
  db.exec("DELETE FROM quotes WHERE id=?",id);db.audit(AuthService.uid(),"DELETE","QUOTE",id,"");
 }
 @PostMapping("/{id}/convert") @Transactional public Map<String,Object> convert(@PathVariable long id){return sales.fromQuote(id);}
}
