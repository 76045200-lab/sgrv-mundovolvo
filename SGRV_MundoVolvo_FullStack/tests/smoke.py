#!/usr/bin/env python3
"""Prueba end-to-end contra una INSTANCIA DE PRUEBAS del SGRV.
REQUIERE SGRV_TEST_ALLOW_WRITE=YES. Crea registros que permanecen en la BD de prueba.
No usar en datos de producción.
"""
import json,os,sys,uuid,urllib.request,urllib.error
from decimal import Decimal
BASE=os.getenv('SGRV_API_URL','http://localhost:8080/api').rstrip('/')
ADMIN=os.getenv('SGRV_ADMIN_USER','')
PASSWORD=os.getenv('SGRV_ADMIN_PASSWORD','')

def request(path,method='GET',body=None,token=None,expect=None):
    req=urllib.request.Request(BASE+path,method=method,data=None if body is None else json.dumps(body).encode(),headers={**({'Content-Type':'application/json'} if body is not None else {}),**({'Authorization':'Bearer '+token} if token else {})})
    try:
        with urllib.request.urlopen(req,timeout=25) as res:
            data=res.read();status=res.status;out=json.loads(data) if data else None
    except urllib.error.HTTPError as exc:
        status=exc.code;data=exc.read();out=json.loads(data) if data else None
    if expect is not None and status!=expect:raise AssertionError(f'{method} {path}: esperado {expect}, obtenido {status}: {out}')
    return out

def check(cond,label):
    if not cond:raise AssertionError(label)
    print('OK',label)

def main():
    if os.getenv('SGRV_TEST_ALLOW_WRITE')!='YES':sys.exit('Esta prueba crea datos: define SGRV_TEST_ALLOW_WRITE=YES en una base aislada')
    if not ADMIN or not PASSWORD:sys.exit('Define SGRV_ADMIN_USER y SGRV_ADMIN_PASSWORD')
    token=request('/auth/login','POST',{'username':ADMIN,'password':PASSWORD},expect=200)['token'];tag='QA-'+uuid.uuid4().hex[:8]
    brand=request('/brands','POST',{'name':'QA Brand '+tag},token,200)
    cat=request('/categories','POST',{'name':'QA Categoria '+tag},token,200)
    product=request('/products','POST',{'code':tag,'name':'Repuesto de prueba '+tag,'brandId':brand['id'],'categoryId':cat['id'],'salePrice':200,'purchaseCost':100,'minStock':2},token,200)
    check(Decimal(str(product['stock']))==0,'Producto creado sin stock ficticio')
    item=request(f"/products/{product['id']}",token=token,expect=200);check(item['code']==tag,'Lectura de producto')
    changed=request(f"/products/{product['id']}",'PUT',{'code':tag,'name':'Repuesto actualizado '+tag,'brandId':brand['id'],'categoryId':cat['id'],'salePrice':200,'purchaseCost':100,'minStock':2},token,200)
    check(changed['name'].startswith('Repuesto actualizado'),'Actualización producto (CRUD)')
    request('/products','POST',{'code':'NEG-'+tag,'name':'Inválido','brandId':brand['id'],'categoryId':cat['id'],'salePrice':-1},token,400)
    print('OK validación negativa de precios')
    customer=request('/customers','POST',{'name':'Cliente de prueba '+tag},token,200)
    supplier=request('/suppliers','POST',{'name':'Proveedor de prueba '+tag},token,200)
    customer=request(f"/customers/{customer['id']}",'PUT',{'name':'Cliente actualizado '+tag},token,200)
    check(customer['name'].startswith('Cliente actualizado'),'Actualización de cliente')
    supplier=request(f"/suppliers/{supplier['id']}",'PUT',{'name':'Proveedor actualizado '+tag},token,200)
    check(supplier['name'].startswith('Proveedor actualizado'),'Actualización de proveedor')
    purchase=request('/purchases','POST',{'supplierId':supplier['id'],'items':[{'productId':product['id'],'quantity':10,'unitCost':100}]},token,200)
    check(request(f"/products/{product['id']}",token=token,expect=200)['stock']==0,'Compra sin recepción no modifica stock')
    receipt=request(f"/purchases/{purchase['id']}/receive",'POST',{'items':[{'purchaseItemId':purchase['items'][0]['id'],'quantity':10}]},token,200)
    check(receipt['status']=='RECEIVED','Recepción completa')
    check(Decimal(str(request(f"/products/{product['id']}",token=token,expect=200)['stock']))==10,'Recepción incrementa existencias')
    request('/sales','POST',{'customerId':customer['id'],'items':[{'productId':product['id'],'quantity':11}]},token,409)
    print('OK, venta sobre stock rechazada')
    quote=request('/quotes','POST',{'customerId':customer['id'],'items':[{'productId':product['id'],'quantity':2}],'discountPct':10},token,200)
    quote=request(f"/quotes/{quote['id']}",'PUT',{'customerId':customer['id'],'items':[{'productId':product['id'],'quantity':2}],'discountPct':10,'notes':'Revisión QA'},token,200)
    check(quote['notes']=='Revisión QA','Edición de borrador de cotización')
    check(Decimal(str(quote['total']))==Decimal('424.80'),'Descuento + IGV 18% calculados por backend')
    request(f"/quotes/{quote['id']}/status",'PATCH',{'status':'SENT'},token,200)
    request(f"/quotes/{quote['id']}/status",'PATCH',{'status':'APPROVED'},token,200)
    sale=request(f"/quotes/{quote['id']}/convert",'POST',{},token,200)
    check(Decimal(str(request(f"/products/{product['id']}",token=token,expect=200)['stock']))==8,'Conversión descuenta stock')
    request(f"/quotes/{quote['id']}/convert",'POST',{},token,409)
    print('OK, doble conversión rechazada')
    request(f"/sales/{sale['id']}/cancel",'POST',{},token,200)
    check(Decimal(str(request(f"/products/{product['id']}",token=token,expect=200)['stock']))==10,'Anulación restaura stock')
    seller=request('/users','POST',{'username':'seller_'+uuid.uuid4().hex[:8],'fullName':'Vendedor QA','password':'test-password-1234','role':'SELLER'},token,200)
    stoken=request('/auth/login','POST',{'username':seller['username'],'password':'test-password-1234'},expect=200)['token']
    request('/products','POST',{'code':'DENIED','name':'No','brandId':brand['id'],'categoryId':cat['id'],'salePrice':1},stoken,403)
    print('OK, vendedor no puede crear productos')
    request('/users',token=stoken,expect=403)
    print('OK, vendedor no puede consultar usuarios')
    isolated=request('/customers','POST',{'name':'Cliente temporal '+tag},token,200)
    request(f"/customers/{isolated['id']}",'DELETE',token=token,expect=200)
    print('OK, eliminación cliente sin operaciones')
    isolated_supplier=request('/suppliers','POST',{'name':'Proveedor temporal '+tag},token,200)
    request(f"/suppliers/{isolated_supplier['id']}",'DELETE',token=token,expect=200)
    print('OK, eliminación proveedor sin operaciones')
    print('\nPRUEBAS COMPLETADAS EN UNA BASE AISLADA. Los registros de QA permanecen en ella.')
if __name__=='__main__':main()
