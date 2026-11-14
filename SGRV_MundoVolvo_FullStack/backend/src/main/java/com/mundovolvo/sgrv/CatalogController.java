package com.mundovolvo.sgrv;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class CatalogController {
 private final Db db;
 CatalogController(Db db){this.db=db;}
 @GetMapping("/brands") public List<Map<String,Object>> brands(){return db.rows("SELECT * FROM brands ORDER BY name");}
 @GetMapping("/categories") public List<Map<String,Object>> categories(){return db.rows("SELECT * FROM categories ORDER BY name");}
 @PostMapping("/{kind:brands|categories}") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public Map<String,Object> addLabel(@PathVariable String kind,@RequestBody Map<String,Object> p){
  long id=db.id("INSERT INTO "+kind+"(name) VALUES(?) RETURNING id",Db.text(p.get("name"),"Nombre",100));db.audit(AuthService.uid(),"CREATE",kind,id,"");return db.one("SELECT * FROM "+kind+" WHERE id=?",id);
 }
 @PutMapping("/{kind:brands|categories}/{id}") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public Map<String,Object> editLabel(@PathVariable String kind,@PathVariable long id,@RequestBody Map<String,Object> p){
  if(db.exec("UPDATE "+kind+" SET name=? WHERE id=?",Db.text(p.get("name"),"Nombre",100),id)==0)throw ApiException.notFound(kind);db.audit(AuthService.uid(),"UPDATE",kind,id,"");return db.one("SELECT * FROM "+kind+" WHERE id=?",id);
 }
 @DeleteMapping("/{kind:brands|categories}/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public void deleteLabel(@PathVariable String kind,@PathVariable long id){if(db.exec("DELETE FROM "+kind+" WHERE id=?",id)==0)throw ApiException.notFound(kind);db.audit(AuthService.uid(),"DELETE",kind,id,"");}
 @GetMapping("/products")
 public List<Map<String,Object>> products(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long brandId){
  StringBuilder sql=new StringBuilder("SELECT p.*,b.name AS brand,c.name AS category FROM products p JOIN brands b ON b.id=p.brand_id JOIN categories c ON c.id=p.category_id WHERE (lower(p.name) LIKE lower(?) OR lower(p.code) LIKE lower(?))");
  List<Object> args=new ArrayList<>();args.add("%"+q+"%");args.add("%"+q+"%");
  if(categoryId!=null){sql.append(" AND p.category_id=?");args.add(categoryId);}
  if(brandId!=null){sql.append(" AND p.brand_id=?");args.add(brandId);}
  sql.append(" ORDER BY p.id DESC LIMIT 5000");return db.rows(sql.toString(),args.toArray());
 }
 @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable long id){return db.one("SELECT p.*,b.name AS brand,c.name AS category FROM products p JOIN brands b ON b.id=p.brand_id JOIN categories c ON c.id=p.category_id WHERE p.id=?",id);}
 @GetMapping("/products/duplicates") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
 public List<Map<String,Object>> duplicates(@RequestParam String code){return db.rows("SELECT p.id,p.code,p.name,b.name AS brand FROM products p JOIN brands b ON b.id=p.brand_id WHERE lower(p.code)=lower(?) ORDER BY p.id",code);}
 @PostMapping("/products") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public Map<String,Object> createProduct(@RequestBody Map<String,Object> p){
  BigDecimal price=decimalNonnegative(p.getOrDefault("salePrice",0),"Precio");
  BigDecimal cost=decimalNonnegative(p.getOrDefault("purchaseCost",0),"Costo");
  if(!"ADMIN".equals(AuthService.actor().get("role"))&&(price.signum()>0||cost.signum()>0))throw ApiException.bad("Solo Administrador puede establecer precios y costos");
  long id=db.id("INSERT INTO products(code,name,description,brand_id,category_id,min_stock,sale_price,purchase_cost) VALUES(?,?,?,?,?,?,?,?) RETURNING id",Db.text(p.get("code"),"Código",90),Db.text(p.get("name"),"Producto",220),Db.nullable(p.get("description")),Db.longVal(p.get("brandId")),Db.longVal(p.get("categoryId")),decimalNonnegative(p.getOrDefault("minStock",0),"Stock mínimo"),price,cost);
  db.audit(AuthService.uid(),"CREATE","PRODUCT",id,"");return product(id);
 }
 @PutMapping("/products/{id}") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public Map<String,Object> editProduct(@PathVariable long id,@RequestBody Map<String,Object> p){
  if(db.exec("UPDATE products SET code=?,name=?,description=?,brand_id=?,category_id=?,min_stock=?,updated_at=now() WHERE id=?",Db.text(p.get("code"),"Código",90),Db.text(p.get("name"),"Producto",220),Db.nullable(p.get("description")),Db.longVal(p.get("brandId")),Db.longVal(p.get("categoryId")),decimalNonnegative(p.getOrDefault("minStock",0),"Stock mínimo"),id)==0)throw ApiException.notFound("Producto");
  if("ADMIN".equals(AuthService.actor().get("role"))&&(p.containsKey("salePrice")||p.containsKey("purchaseCost"))){
   Map<String,Object> current=db.one("SELECT sale_price,purchase_cost FROM products WHERE id=?",id);
   BigDecimal price=p.containsKey("salePrice")?decimalNonnegative(p.get("salePrice"),"Precio de venta"):(BigDecimal)current.get("sale_price");
   BigDecimal cost=p.containsKey("purchaseCost")?decimalNonnegative(p.get("purchaseCost"),"Costo"):(BigDecimal)current.get("purchase_cost");
   db.exec("UPDATE products SET sale_price=?,purchase_cost=? WHERE id=?",price,cost,id);
   db.audit(AuthService.uid(),"PRICE_CHANGE","PRODUCT",id,"Edición autorizada de precios");
  }
  db.audit(AuthService.uid(),"UPDATE","PRODUCT",id,"");return product(id);
 }
 @PutMapping("/products/{id}/prices") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Map<String,Object> prices(@PathVariable long id,@RequestBody Map<String,Object> p){
  BigDecimal price=decimalNonnegative(p.get("salePrice"),"Precio de venta");BigDecimal cost=decimalNonnegative(p.get("purchaseCost"),"Costo");
  if(db.exec("UPDATE products SET sale_price=?,purchase_cost=?,updated_at=now() WHERE id=?",price,cost,id)==0)throw ApiException.notFound("Producto");
  db.audit(AuthService.uid(),"PRICE_CHANGE","PRODUCT",id,"Actualización de costo y precio");return product(id);
 }
 @DeleteMapping("/products/{id}") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") @Transactional
 public void deleteProduct(@PathVariable long id){
  Map<String,Object> product=db.one("SELECT stock FROM products WHERE id=? FOR UPDATE",id);
  if(((BigDecimal)product.get("stock")).signum()!=0)throw ApiException.conflict("Primero regularice el stock a cero");
  db.exec("DELETE FROM products WHERE id=?",id);db.audit(AuthService.uid(),"DELETE","PRODUCT",id,"");
 }
 @GetMapping("/{kind:customers|suppliers}") @PreAuthorize("hasAnyRole('ADMIN','SELLER') or (hasRole('WAREHOUSE') and #kind == 'suppliers')")
 public List<Map<String,Object>> parties(@PathVariable String kind,@RequestParam(defaultValue="") String q){return db.rows("SELECT * FROM "+kind+" WHERE lower(name) LIKE lower(?) ORDER BY id DESC LIMIT 1000","%"+q+"%");}
 @PostMapping("/{kind:customers|suppliers}") @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and #kind == 'customers')") @Transactional
 public Map<String,Object> addParty(@PathVariable String kind,@RequestBody Map<String,Object> p){
  String extra=kind.equals("customers")?"document":"tax_id";
  long id=db.id("INSERT INTO "+kind+"(name,"+extra+",phone,email,address) VALUES(?,?,?,?,?) RETURNING id",Db.text(p.get("name"),"Nombre",200),Db.nullable(p.get(kind.equals("customers")?"document":"taxId")),Db.nullable(p.get("phone")),Db.nullable(p.get("email")),Db.nullable(p.get("address")));
  db.audit(AuthService.uid(),"CREATE",kind,id,"");return db.one("SELECT * FROM "+kind+" WHERE id=?",id);
 }
 @PutMapping("/{kind:customers|suppliers}/{id}") @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and #kind == 'customers')") @Transactional
 public Map<String,Object> editParty(@PathVariable String kind,@PathVariable long id,@RequestBody Map<String,Object> p){
  String extra=kind.equals("customers")?"document":"tax_id";
  if(db.exec("UPDATE "+kind+" SET name=?,"+extra+"=?,phone=?,email=?,address=?,active=? WHERE id=?",Db.text(p.get("name"),"Nombre",200),Db.nullable(p.get(kind.equals("customers")?"document":"taxId")),Db.nullable(p.get("phone")),Db.nullable(p.get("email")),Db.nullable(p.get("address")),!Boolean.FALSE.equals(p.get("active")),id)==0)throw ApiException.notFound(kind);
  db.audit(AuthService.uid(),"UPDATE",kind,id,"");return db.one("SELECT * FROM "+kind+" WHERE id=?",id);
 }
 @DeleteMapping("/{kind:customers|suppliers}/{id}") @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and #kind == 'customers')") @Transactional
 public void deleteParty(@PathVariable String kind,@PathVariable long id){if(db.exec("DELETE FROM "+kind+" WHERE id=?",id)==0)throw ApiException.notFound(kind);db.audit(AuthService.uid(),"DELETE",kind,id,"");}
 public static BigDecimal decimalNonnegative(Object val,String field){BigDecimal n=Db.decimal(val,field);if(n.signum()<0)throw ApiException.bad(field+" no puede ser negativo");return n;}
}
