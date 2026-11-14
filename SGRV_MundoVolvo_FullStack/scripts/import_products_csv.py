#!/usr/bin/env python3
"""Importa únicamente datos comerciales actuales y autorizados.

No usar directamente el Excel histórico de 2025: pertenece a otra razón social
según su encabezado, expresa precios en USD y su stock no es un conteo actual.

CSV encabezados: code,name,brand,category,description,sale_price_pen,purchase_cost_pen,min_stock,initial_stock
Este proceso crea marcas, categorías y productos y asienta stock inicial en auditoría.
NO es atómico a nivel de archivo; ante fallo, revise el resultado antes de reintentar.
"""
import csv, json, os, sys, urllib.request, urllib.error
from decimal import Decimal
BASE = os.getenv('SGRV_API_URL','http://localhost:8080/api').rstrip('/')
USERNAME = os.getenv('SGRV_ADMIN_USER','')
PASSWORD = os.getenv('SGRV_ADMIN_PASSWORD','')

def call(path, method='GET', payload=None, token=None):
    request=urllib.request.Request(BASE+path, data=None if payload is None else json.dumps(payload).encode(), method=method, headers={**({'Content-Type':'application/json'} if payload is not None else {}),**({'Authorization':'Bearer '+token} if token else {})})
    with urllib.request.urlopen(request,timeout=30) as result:
        content=result.read()
        return json.loads(content) if content else None

def main(path):
    if os.getenv('SGRV_IMPORT_AUTHORIZED') != 'YES':
        sys.exit('Confirma la carga autorizada definiendo SGRV_IMPORT_AUTHORIZED=YES')
    if not USERNAME or not PASSWORD:sys.exit('Define SGRV_ADMIN_USER y SGRV_ADMIN_PASSWORD')
    token=call('/auth/login','POST',{'username':USERNAME,'password':PASSWORD})['token']
    brands={x['name'].casefold():x['id'] for x in call('/brands',token=token)}
    categories={x['name'].casefold():x['id'] for x in call('/categories',token=token)}
    existing={(p['code'].casefold(),p['brand'].casefold(),p['name'].casefold()) for p in call('/products',token=token)}
    success=0;errors=[]
    with open(path,newline='',encoding='utf-8-sig') as handle:
        for rownumber,row in enumerate(csv.DictReader(handle),start=2):
            try:
                for field in ['code','name','brand','category','sale_price_pen','purchase_cost_pen','min_stock','initial_stock']:
                    if not (row.get(field) or '').strip():raise ValueError('Falta '+field)
                key=(row['code'].casefold(),row['brand'].casefold(),row['name'].casefold())
                if key in existing:raise ValueError('Producto coincidente: revisar duplicado manualmente')
                name=row['brand'].strip(); b=brands.get(name.casefold())
                if not b:
                    b=call('/brands','POST',{'name':name},token)['id'];brands[name.casefold()]=b
                cat=row['category'].strip();c=categories.get(cat.casefold())
                if not c:
                    c=call('/categories','POST',{'name':cat},token)['id'];categories[cat.casefold()]=c
                for field in ['sale_price_pen','purchase_cost_pen','min_stock','initial_stock']:
                    if Decimal(row[field])<0:raise ValueError(field+' negativo')
                product=call('/products','POST',{'code':row['code'],'name':row['name'],'brandId':b,'categoryId':c,'description':row.get('description',''),'salePrice':row['sale_price_pen'],'purchaseCost':row['purchase_cost_pen'],'minStock':row['min_stock']},token)
                stock=Decimal(row['initial_stock'])
                if stock>0:call('/stock/adjust','POST',{'productId':product['id'],'direction':'IN','quantity':str(stock),'note':'Conteo físico autorizado de apertura (importación CSV)'},token)
                existing.add(key);success+=1
            except Exception as ex:errors.append((rownumber,str(ex)))
    print(f'Importados: {success}, errores: {len(errors)}')
    for line,error in errors:print('Fila',line,':',error)
    if errors:sys.exit(1)
if __name__=='__main__':
    if len(sys.argv)!=2:sys.exit('Uso: python scripts/import_products_csv.py archivo.csv')
    main(sys.argv[1])
