import { Injectable, inject } from '@angular/core';import { ApiService } from './api.service';
@Injectable({providedIn:'root'}) export class ProductService {
 private api=inject(ApiService);
 list(q='',categoryId='',brandId=''){const params=new URLSearchParams({q});if(categoryId)params.set('categoryId',categoryId);if(brandId)params.set('brandId',brandId);return this.api.get<any[]>('/products?'+params.toString());}
 duplicates(code:string){return this.api.get<any[]>('/products/duplicates?code='+encodeURIComponent(code))}
 get(id:number){return this.api.get<any>(`/products/${id}`)}
 create(data:any){return this.api.post('/products',data)}
 update(id:number,data:any){return this.api.put(`/products/${id}`,data)}
 updatePrices(id:number,data:any){return this.api.put(`/products/${id}/prices`,data)}
 remove(id:number){return this.api.delete(`/products/${id}`)}
 brands(){return this.api.get<any[]>('/brands')}
 categories(){return this.api.get<any[]>('/categories')}
 createLabel(kind:string,name:string){return this.api.post(`/${kind}`,{name})}
 updateLabel(kind:string,id:number,name:string){return this.api.put(`/${kind}/${id}`,{name})}
 removeLabel(kind:string,id:number){return this.api.delete(`/${kind}/${id}`)}
 history(productId?:number){return this.api.get<any[]>('/stock/movements'+(productId?'?productId='+productId:''))}
 adjust(data:any){return this.api.post('/stock/adjust',data)}
}
