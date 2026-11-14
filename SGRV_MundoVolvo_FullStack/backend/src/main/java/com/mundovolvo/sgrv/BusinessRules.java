package com.mundovolvo.sgrv;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessRules {
 private final Db db;
 BusinessRules(Db db){this.db=db;}
 public static final BigDecimal IGV=new BigDecimal("0.18");
 public record Totals(BigDecimal subtotal,BigDecimal discountAmount,BigDecimal tax,BigDecimal total){}
 public Totals calculate(List<Map<String,Object>> items,BigDecimal discountPct){
  if(discountPct.compareTo(BigDecimal.ZERO)<0||discountPct.compareTo(new BigDecimal("100"))>0)throw ApiException.bad("Descuento debe estar entre 0 y 100");
  BigDecimal subtotal=BigDecimal.ZERO;
  for(Map<String,Object> item:items){subtotal=subtotal.add(Db.decimal(item.get("line_total"),"Subtotal"));}
  subtotal=subtotal.setScale(2,RoundingMode.HALF_UP);
  BigDecimal discount=subtotal.multiply(discountPct).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
  BigDecimal base=subtotal.subtract(discount);
  BigDecimal tax=base.multiply(IGV).setScale(2,RoundingMode.HALF_UP);
  return new Totals(subtotal,discount,tax,base.add(tax));
 }
 public List<Map<String,Object>> lines(Object raw,boolean quote){
  if(!(raw instanceof List<?> array)||array.isEmpty())throw ApiException.bad("Añade al menos un producto");
  if(array.size()>200)throw ApiException.bad("Máximo 200 líneas por operación");
  List<Map<String,Object>> result=new ArrayList<>();Set<Long> unique=new HashSet<>();
  for(Object obj:array){
   if(!(obj instanceof Map<?,?> row))throw ApiException.bad("Línea inválida");
   long productId=Db.longVal(row.get("productId"));if(!unique.add(productId))throw ApiException.bad("No repitas un producto en la misma operación");
   BigDecimal quantity=Db.decimal(row.get("quantity"),"Cantidad");if(quantity.signum()<=0||quantity.scale()>3)throw ApiException.bad("Cantidad debe ser positiva, máximo tres decimales");
   Map<String,Object> product=db.one("SELECT id,code,name,stock,sale_price,active FROM products WHERE id=?",productId);
   if(!Boolean.TRUE.equals(product.get("active")))throw ApiException.bad("Producto inactivo: "+product.get("name"));
   BigDecimal unit=(BigDecimal)product.get("sale_price");if(unit.signum()<=0)throw ApiException.bad("Producto sin precio de venta vigente: "+product.get("name"));
   Map<String,Object> line=new HashMap<>();line.put("product_id",productId);line.put("product_code",product.get("code"));line.put("product_name",product.get("name"));line.put("quantity",quantity);line.put("unit_price",unit);line.put("line_total",unit.multiply(quantity).setScale(2,RoundingMode.HALF_UP));result.add(line);
  }
  return result;
 }
 @Transactional public BigDecimal move(long productId,BigDecimal quantity,String direction,String refType,long refId,String note){
  if(quantity.signum()<=0||quantity.scale()>3)throw ApiException.bad("Cantidad debe ser mayor que cero y máximo tres decimales");
  Map<String,Object> p=db.one("SELECT stock,active FROM products WHERE id=? FOR UPDATE",productId);
  BigDecimal current=(BigDecimal)p.get("stock");BigDecimal next="IN".equals(direction)?current.add(quantity):current.subtract(quantity);
  if(next.signum()<0)throw ApiException.conflict("Stock insuficiente para el producto ID "+productId+". Existencias: "+current);
  db.exec("UPDATE products SET stock=?,updated_at=now() WHERE id=?",next,productId);
  db.exec("INSERT INTO stock_movements(product_id,direction,quantity,balance_after,reference_type,reference_id,note,created_by) VALUES(?,?,?,?,?,?,?,?)",productId,direction,quantity,next,refType,refId,note,AuthService.uid());
  return next;
 }
}
