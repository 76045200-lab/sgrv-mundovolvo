package com.mundovolvo.sgrv;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {
 private final Db db;private final BusinessRules rules;
 SaleService(Db db,BusinessRules rules){this.db=db;this.rules=rules;}
 @Transactional public Map<String,Object> create(Map<String,Object> p){
  long customerId=Db.longVal(p.get("customerId"));
  BigDecimal pct=Db.decimal(p.getOrDefault("discountPct",0),"Descuento");
  List<Map<String,Object>> lines=rules.lines(p.get("items"),false);return persist(customerId,pct,lines,null);
 }
 @Transactional public Map<String,Object> fromQuote(long quoteId){
  Map<String,Object> q=db.one("SELECT * FROM quotes WHERE id=? FOR UPDATE",quoteId);
  if(!"APPROVED".equals(q.get("status"))||q.get("converted_sale_id")!=null)throw ApiException.conflict("La cotización no está aprobada o ya se convirtió");
  List<Map<String,Object>> lines=db.rows("SELECT product_id,product_code,product_name,quantity,unit_price,line_total FROM quote_items WHERE quote_id=? ORDER BY product_id",quoteId);
  Map<String,Object> sale=persist(Db.longVal(q.get("customer_id")),(BigDecimal)q.get("discount_pct"),lines,quoteId);
  db.exec("UPDATE quotes SET status='CONVERTED', converted_sale_id=?,updated_at=now() WHERE id=?",sale.get("id"),quoteId);
  db.audit(AuthService.uid(),"CONVERT","QUOTE",quoteId,"Venta "+sale.get("id"));return sale;
 }
 private Map<String,Object> persist(long customerId,BigDecimal discountPct,List<Map<String,Object>> lines,Long quoteId){
  Map<String,Object> customer=db.one("SELECT active FROM customers WHERE id=?",customerId);if(!Boolean.TRUE.equals(customer.get("active")))throw ApiException.bad("Cliente inactivo");
  BusinessRules.Totals total=rules.calculate(lines,discountPct);
  // Lock products in fixed order to prevent concurrent overselling and reduce deadlocks.
  List<Map<String,Object>> sorted=new ArrayList<>(lines);sorted.sort(Comparator.comparingLong(x->Db.longVal(x.get("product_id"))));
  for(Map<String,Object> line:sorted){
   Map<String,Object> p=db.one("SELECT stock,active FROM products WHERE id=? FOR UPDATE",line.get("product_id"));
   if(!Boolean.TRUE.equals(p.get("active")))throw ApiException.bad("Producto inactivo: "+line.get("product_name"));
   if(((BigDecimal)p.get("stock")).compareTo((BigDecimal)line.get("quantity"))<0)throw ApiException.conflict("Stock insuficiente de "+line.get("product_name"));
  }
  long id=db.id("INSERT INTO sales(customer_id,quote_id,subtotal,discount_pct,discount_amount,tax,total,created_by) VALUES(?,?,?,?,?,?,?,?) RETURNING id",customerId,quoteId,total.subtotal(),discountPct,total.discountAmount(),total.tax(),total.total(),AuthService.uid());
  db.exec("UPDATE sales SET sale_number='VEN-'||lpad(id::text,6,'0') WHERE id=?",id);
  for(Map<String,Object> line:sorted){
   db.exec("INSERT INTO sale_items(sale_id,product_id,product_code,product_name,quantity,unit_price,line_total) VALUES(?,?,?,?,?,?,?)",id,line.get("product_id"),line.get("product_code"),line.get("product_name"),line.get("quantity"),line.get("unit_price"),line.get("line_total"));
   rules.move(Db.longVal(line.get("product_id")),(BigDecimal)line.get("quantity"),"OUT","SALE",id,"Venta confirmada");
  }
  db.audit(AuthService.uid(),"CREATE","SALE",id,"");return get(id);
 }
 public Map<String,Object> get(long id){Map<String,Object> sale=new LinkedHashMap<>(db.one("SELECT s.*,c.name AS customer_name,c.document AS customer_document,c.address AS customer_address FROM sales s JOIN customers c ON c.id=s.customer_id WHERE s.id=?",id));sale.put("items",db.rows("SELECT * FROM sale_items WHERE sale_id=? ORDER BY id",id));return sale;}
 @Transactional public Map<String,Object> cancel(long id){
  Map<String,Object> sale=db.one("SELECT status FROM sales WHERE id=? FOR UPDATE",id);
  if(!"CONFIRMED".equals(sale.get("status")))throw ApiException.conflict("Esta venta ya fue anulada");
  List<Map<String,Object>> items=db.rows("SELECT product_id,quantity FROM sale_items WHERE sale_id=? ORDER BY product_id",id);
  for(Map<String,Object> item:items)rules.move(Db.longVal(item.get("product_id")),(BigDecimal)item.get("quantity"),"IN","SALE_CANCEL",id,"Reversión por anulación");
  db.exec("UPDATE sales SET status='CANCELLED',cancelled_at=now() WHERE id=?",id);
  db.audit(AuthService.uid(),"CANCEL","SALE",id,"");return get(id);
 }
}
