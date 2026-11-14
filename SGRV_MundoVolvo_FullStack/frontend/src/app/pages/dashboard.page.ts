import {Component,OnInit,inject} from '@angular/core';import {CommonModule,DecimalPipe,CurrencyPipe} from '@angular/common';import {BusinessService} from '../services/business.service';import {AuthService} from '../services/auth.service';
@Component({selector:'app-dashboard',standalone:true,imports:[CommonModule],template:`
<div class="flex flex-wrap items-end justify-between gap-3 mb-7"><div><h1 class="page-title">Resumen general</h1><p class="text-slate-500 text-sm mt-1">Información actual almacenada en el sistema, sin datos simulados.</p></div><button class="btn-soft" (click)="load()">↻ Actualizar</button></div>
@if(error){<div class="notice-error">{{error}}</div>}
@if(loading){<div class="panel">Consultando indicadores...</div>}
@if(data&&!loading){
 <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
  @for(c of cards;track c.label){<div class="panel relative overflow-hidden"><div class="text-xs font-semibold tracking-wide uppercase text-slate-500">{{c.label}}</div><div class="text-3xl font-bold text-[#0c1d34] mt-3">{{c.currency?(c.value|currency:'PEN':'symbol':'1.2-2'):(c.value|number:'1.0-0')}}</div><div class="absolute right-4 top-4 text-3xl opacity-20">{{c.icon}}</div></div>}
 </div>
 @if(auth.user()?.role!=='WAREHOUSE'){<div class="panel"><div class="flex justify-between items-center mb-4"><div><h2 class="font-bold text-lg text-slate-800">Últimas cotizaciones</h2><p class="text-slate-500 text-xs mt-1">Movimientos recientes registrados</p></div><span class="badge bg-sky-50 text-sky-700">En vivo</span></div>
 <div class="table-wrap"><table class="data-table"><thead><tr><th>Número</th><th>Cliente</th><th>Fecha</th><th>Estado</th><th class="text-right">Total</th></tr></thead><tbody>
 @for(q of data.recentQuotes;track q.id){<tr><td class="font-semibold">{{q.quote_number}}</td><td>{{q.customer_name}}</td><td>{{q.created_at|date:'dd/MM/yyyy'}}</td><td><span class="badge" [ngClass]="q.status==='APPROVED'?'bg-emerald-100 text-emerald-700':q.status==='REJECTED'?'bg-red-100 text-red-700':'bg-slate-100 text-slate-700'">{{status(q.status)}}</span></td><td class="text-right font-semibold">{{q.total|currency:'PEN':'symbol'}}</td></tr>}
 @empty{<tr><td colspan="5" class="text-center py-10 text-slate-400">Aún no hay cotizaciones registradas.</td></tr>}
 </tbody></table></div></div>}
}
`})export class DashboardPage implements OnInit {
 private service=inject(BusinessService);auth=inject(AuthService);data:any;loading=true;error='';cards:any[]=[];
 ngOnInit(){this.load()}
 async load(){this.loading=true;this.error='';try{this.data=await this.service.dashboard();this.cards=[{label:'Productos activos',value:this.data.products,icon:'▤'},{label:'Stock bajo',value:this.data.lowStock,icon:'⚠'},...(this.auth.user()?.role==='WAREHOUSE'?[]:[{label:'Cotizaciones del mes',value:this.data.quotesThisMonth,icon:'▧'},{label:'Ventas del mes',value:this.data.salesThisMonth,currency:true,icon:'◈'}])]}catch(e:any){this.error=e.message}finally{this.loading=false}}
 status(s:string){return ({DRAFT:'Borrador',SENT:'Enviada',APPROVED:'Aprobada',REJECTED:'Rechazada',CONVERTED:'Convertida'} as any)[s]||s}
}
