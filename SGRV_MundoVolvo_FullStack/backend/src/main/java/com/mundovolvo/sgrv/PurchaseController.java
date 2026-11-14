package com.mundovolvo.sgrv;

import java.math.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api/purchases")
@PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
public class PurchaseController {
 private final Db db;private final BusinessRules rules;
 PurchaseController(Db db,BusinessRules rules){this.db=db;this.rules=rules;}
 private void validateSupplier(long id){Map<String,Object> sp=db.one("SELECT active FROM suppliers WHERE id=?",id);if(!Boolean.TRUE.equals(sp.get("active")))throw ApiException.bad("Proveedor inactivo");}
 @GetMapping public List<Map<String,Object>> list(){return db.rows("SELECT p.*,s.name AS supplier_name FROM purchases p JOIN suppliers s ON s.id=p.supplier_id ORDER BY p.id DESC LIMIT 1000");}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable long id){Map<String,Object> p=new LinkedHashMap<>(db.one("SELECT p.*,s.name AS supplier_name FROM purchases p JOIN suppliers s ON s.id=p.supplier_id WHERE p.id=?",id));p.put("items",db.rows("SELECT pi.*,pr.code,pr.name AS product_name FROM purchase_items pi JOIN products pr ON pr.id=pi.product_id WHERE pi.purchase_id=? ORDER BY pi.id",id));p.put("receipts",db.rows("SELECT * FROM purchase_receipts WHERE purchase_id=? ORDER BY id DESC",id));return p;}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @Transactional public Map<String,Object> create(@RequestBody Map<String,Object> p){
  long supplierId=Db.longVal(p.get("supplierId"));validateSupplier(supplierId);
  if(!(p.get("items") instanceof List<?> raw)||raw.isEmpty())throw ApiException.bad("Compra sin productos");
  Set<Long> unique=new HashSet<>();BigDecimal total=BigDecimal.ZERO;List<Map<String,Object>> data=new ArrayList<>();
  for(Object x:raw){if(!(x instanceof Map<?,?> row))throw ApiException.bad("Línea inválida");long productId=Db.longVal(row.get("productId"));if(!unique.add(productId))throw ApiException.bad("Producto repetido");
   db.one("SELECT id FROM products WHERE id=?",productId);
   BigDecimal qty=Db.decimal(row.get("quantity"),"Cantidad"),cost=CatalogController.decimalNonnegative(row.get("unitCost"),"Costo");
   if(qty.signum()<=0||qty.scale()>3)throw ApiException.bad("Cantidad debe ser positiva, máximo tres decimales");
   BigDecimal line=qty.multiply(cost).setScale(2,RoundingMode.HALF_UP);total=total.add(line);
   data.add(Map.of("productId",productId,"quantity",qty,"unitCost",cost,"lineTotal",line));
  }
  long id=db.id("INSERT INTO purchases(supplier_id,total,notes,created_by) VALUES(?,?,?,?) RETURNING id",supplierId,total,Db.nullable(p.get("notes")),AuthService.uid());
  db.exec("UPDATE purchases SET purchase_number='COM-'||lpad(id::text,6,'0') WHERE id=?",id);
  for(Map<String,Object> d:data)db.exec("INSERT INTO purchase_items(purchase_id,product_id,quantity,unit_cost,line_total) VALUES(?,?,?,?,?)",id,d.get("productId"),d.get("quantity"),d.get("unitCost"),d.get("lineTotal"));
  db.audit(AuthService.uid(),"CREATE","PURCHASE",id,"");return get(id);
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> p){
  Map<String,Object> old=db.one("SELECT status FROM purchases WHERE id=? FOR UPDATE",id);
  if(!"PENDING".equals(old.get("status")))throw ApiException.conflict("Solo se modifican compras sin recepciones");
  if(!(p.get("items") instanceof List<?> raw)||raw.isEmpty())throw ApiException.bad("Compra sin productos");
  Set<Long> unique=new HashSet<>();BigDecimal total=BigDecimal.ZERO;List<Map<String,Object>> lines=new ArrayList<>();
  for(Object obj:raw){if(!(obj instanceof Map<?,?> row))throw ApiException.bad("Línea inválida");long pid=Db.longVal(row.get("productId"));if(!unique.add(pid))throw ApiException.bad("Producto repetido");
   db.one("SELECT id FROM products WHERE id=?",pid);
   BigDecimal qty=Db.decimal(row.get("quantity"),"Cantidad"),cost=CatalogController.decimalNonnegative(row.get("unitCost"),"Costo");
   if(qty.signum()<=0||qty.scale()>3)throw ApiException.bad("Cantidad inválida");
   BigDecimal amount=qty.multiply(cost).setScale(2,RoundingMode.HALF_UP);total=total.add(amount);
   lines.add(Map.of("pid",pid,"qty",qty,"cost",cost,"amount",amount));
  }
  validateSupplier(Db.longVal(p.get("supplierId")));
  db.exec("UPDATE purchases SET supplier_id=?,total=?,notes=? WHERE id=?",Db.longVal(p.get("supplierId")),total,Db.nullable(p.get("notes")),id);
  db.exec("DELETE FROM purchase_items WHERE purchase_id=?",id);
  for(Map<String,Object> x:lines)db.exec("INSERT INTO purchase_items(purchase_id,product_id,quantity,unit_cost,line_total) VALUES(?,?,?,?,?)",id,x.get("pid"),x.get("qty"),x.get("cost"),x.get("amount"));
  db.audit(AuthService.uid(),"UPDATE","PURCHASE",id,"");return get(id);
 }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional public void delete(@PathVariable long id){
  Map<String,Object> purchase=db.one("SELECT status FROM purchases WHERE id=? FOR UPDATE",id);
  if(!"PENDING".equals(purchase.get("status")))throw ApiException.conflict("No se elimina una compra con recepciones");
  db.exec("DELETE FROM purchases WHERE id=?",id);db.audit(AuthService.uid(),"DELETE","PURCHASE",id,"");
 }
 @PostMapping("/{id}/receive") @Transactional public Map<String,Object> receive(@PathVariable long id,@RequestBody Map<String,Object> p){
  Map<String,Object> purchase=db.one("SELECT status FROM purchases WHERE id=? FOR UPDATE",id);
  if("RECEIVED".equals(purchase.get("status")))throw ApiException.conflict("Compra completamente recibida");
  if(!(p.get("items") instanceof List<?> raw)||raw.isEmpty())throw ApiException.bad("Indica cantidades recibidas");
  Set<Long> unique=new HashSet<>();List<Map<String,Object>> changes=new ArrayList<>();
  for(Object o:raw){if(!(o instanceof Map<?,?> x))throw ApiException.bad("Línea inválida");
   long itemId=Db.longVal(x.get("purchaseItemId"));if(!unique.add(itemId))throw ApiException.bad("Línea repetida");
   Map<String,Object> item=db.one("SELECT * FROM purchase_items WHERE id=? AND purchase_id=? FOR UPDATE",itemId,id);
   BigDecimal qty=Db.decimal(x.get("quantity"),"Recibido");if(qty.signum()<=0||qty.scale()>3)throw ApiException.bad("Recibido debe ser positivo, máximo tres decimales");
   BigDecimal left=((BigDecimal)item.get("quantity")).subtract((BigDecimal)item.get("received_qty"));if(qty.compareTo(left)>0)throw ApiException.conflict("La recepción excede la cantidad pendiente");
   changes.add(Map.of("itemId",itemId,"productId",item.get("product_id"),"quantity",qty));
  }
  long receiptId=db.id("INSERT INTO purchase_receipts(purchase_id,created_by) VALUES(?,?) RETURNING id",id,AuthService.uid());
  changes.sort(Comparator.comparingLong(m->Db.longVal(m.get("productId"))));
  for(Map<String,Object> change:changes){
   db.exec("INSERT INTO purchase_receipt_items(receipt_id,purchase_item_id,quantity) VALUES(?,?,?)",receiptId,change.get("itemId"),change.get("quantity"));
   db.exec("UPDATE purchase_items SET received_qty=received_qty+? WHERE id=?",change.get("quantity"),change.get("itemId"));
   rules.move(Db.longVal(change.get("productId")),(BigDecimal)change.get("quantity"),"IN","PURCHASE_RECEIPT",receiptId,"Recepción de compra");
  }
  BigDecimal pending=db.num("SELECT coalesce(sum(quantity-received_qty),0) FROM purchase_items WHERE purchase_id=?",id);
  db.exec("UPDATE purchases SET status=? WHERE id=?",pending.signum()==0?"RECEIVED":"PARTIAL",id);
  db.audit(AuthService.uid(),"RECEIVE","PURCHASE",id,"Recepción "+receiptId);return get(id);
 }
}
