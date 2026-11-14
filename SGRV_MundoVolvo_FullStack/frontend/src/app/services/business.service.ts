import { Injectable, inject } from '@angular/core';import { ApiService } from './api.service';
@Injectable({providedIn:'root'})export class BusinessService{
 private api=inject(ApiService);
 list(kind:'customers'|'suppliers'|'quotes'|'sales'|'purchases'){return this.api.get<any[]>(`/${kind}`)}
 get(kind:'quotes'|'sales'|'purchases',id:number){return this.api.get<any>(`/${kind}/${id}`)}
 create(kind:string,data:any){return this.api.post<any>(`/${kind}`,data)}
 update(kind:string,id:number,data:any){return this.api.put<any>(`/${kind}/${id}`,data)}
 remove(kind:string,id:number){return this.api.delete(`/${kind}/${id}`)}
 quoteStatus(id:number,status:string){return this.api.patch(`/quotes/${id}/status`,{status})}
 convert(id:number){return this.api.post<any>(`/quotes/${id}/convert`,{})}
 cancelSale(id:number){return this.api.post(`/sales/${id}/cancel`,{})}
 receive(id:number,items:any[]){return this.api.post(`/purchases/${id}/receive`,{items})}
 dashboard(){return this.api.get<any>('/dashboard')}
 report(kind:string){return this.api.get<any[]>(`/reports/${kind}`)}
 settings(){return this.api.get<any[]>('/settings')}
 audit(){return this.api.get<any[]>('/audit')}
}
