package com.mundovolvo.sgrv;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api/stock")
public class StockController {
 private final Db db;private final BusinessRules rules;
 StockController(Db db,BusinessRules rules){this.db=db;this.rules=rules;}
 @GetMapping("/movements") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
 public List<Map<String,Object>> history(@RequestParam(required=false) Long productId){
  String sql="SELECT m.*,p.code,p.name AS product_name,u.username AS actor FROM stock_movements m JOIN products p ON p.id=m.product_id JOIN app_users u ON u.id=m.created_by";
  return productId==null?db.rows(sql+" ORDER BY m.id DESC LIMIT 1000"):db.rows(sql+" WHERE m.product_id=? ORDER BY m.id DESC LIMIT 1000",productId);
 }
 @PostMapping("/adjust") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public Map<String,Object> adjust(@RequestBody Map<String,Object> p){
  long productId=Db.longVal(p.get("productId"));String direction=Db.text(p.get("direction"),"Movimiento",5);
  if(!Set.of("IN","OUT").contains(direction))throw ApiException.bad("Movimiento debe ser IN o OUT");
  String note=Db.text(p.get("note"),"Justificación",400);
  BigDecimal quantity=Db.decimal(p.get("quantity"),"Cantidad");
  long id=db.id("SELECT nextval('stock_movements_id_seq')");
  // Reference uses movement ID; the sequence value is explicitly reserved.
  Map<String,Object> prod=db.one("SELECT stock FROM products WHERE id=? FOR UPDATE",productId);
  BigDecimal next="IN".equals(direction)?((BigDecimal)prod.get("stock")).add(quantity):((BigDecimal)prod.get("stock")).subtract(quantity);
  if(quantity.signum()<=0||quantity.scale()>3||next.signum()<0)throw ApiException.bad("Cantidad inválida o stock insuficiente");
  db.exec("UPDATE products SET stock=?,updated_at=now() WHERE id=?",next,productId);
  db.exec("INSERT INTO stock_movements(id,product_id,direction,quantity,balance_after,reference_type,reference_id,note,created_by) VALUES(?,?,?,?,?,'MANUAL',?,?,?)",id,productId,direction,quantity,next,id,note,AuthService.uid());
  db.audit(AuthService.uid(),"ADJUST","PRODUCT",productId,note);
  return Map.of("movementId",id,"stock",next);
 }
}
