import {Component,OnInit,inject} from '@angular/core';import {CommonModule} from '@angular/common';import {BusinessService} from '../services/business.service';import {AuthService} from '../services/auth.service';
@Component({selector:'app-reports',standalone:true,imports:[CommonModule],template:`
<div class="flex flex-wrap justify-between items-center gap-3 mb-6"><div><h1 class="page-title">Reportes gerenciales</h1><p class="text-sm text-slate-500 mt-1">Resultados calculados a partir de transacciones confirmadas.</p></div><button class="btn-soft" (click)="load()">↻ Actualizar</button></div>
@if(error){<div class="notice-error">{{error}}</div>}
<div class="flex gap-2 overflow-x-auto mb-5">@for(t of tabs;track t.key){<button class="btn whitespace-nowrap" [ngClass]="tab===t.key?'bg-[#116aa8] text-white':'bg-white border border-slate-200'" (click)="tab=t.key;load()">{{t.label}}</button>}</div>
<div class="panel"><h2 class="font-bold text-lg mb-3">{{tabTitle}}</h2>@if(loading){<p class="text-slate-500 py-10">Consultando base de datos...</p>}@else{
<div class="table-wrap"><table class="data-table"><thead><tr>@if(tab==='sales'||tab==='purchases'){<th>Mes</th><th>Operaciones</th><th>Total S/</th>}
@if(tab==='inventory'){<th>Código</th><th>Producto</th><th>Marca</th><th>Stock</th><th>Mínimo</th><th>Valor de stock (costo) S/</th>}
@if(tab==='rotation'){<th>Código</th><th>Producto</th><th>Stock inicial</th><th>Stock actual</th><th>Vendido</th><th>Rotación</th>}
@if(tab==='abc'){<th>Código</th><th>Producto</th><th>Ventas S/</th><th>Unidades vendidas</th><th>Clasificación ABC</th>}
</tr></thead><tbody>@for(r of rows;track $index){<tr>
@if(tab==='sales'||tab==='purchases'){<td>{{r.month|date:'MM/yyyy'}}</td><td>{{r.operations}}</td><td>{{r.total|number:'1.2-2'}}</td>}
@if(tab==='inventory'){<td>{{r.code}}</td><td>{{r.name}}</td><td>{{r.brand}}</td><td>{{r.stock}}</td><td>{{r.min_stock}}</td><td>{{(+r.stock)*(+r.purchase_cost)|number:'1.2-2'}}</td>}
@if(tab==='rotation'){<td>{{r.code}}</td><td>{{r.name}}</td><td>{{r.opening_stock}}</td><td>{{r.stock}}</td><td>{{r.sold_qty}}</td><td>{{r.turnover===null?'N/D':r.turnover}}</td>}
@if(tab==='abc'){<td>{{r.code}}</td><td>{{r.name}}</td><td>{{r.revenue|number:'1.2-2'}}</td><td>{{r.units}}</td><td><span class="badge bg-blue-50 text-blue-700">{{r.abc}}</span></td>}
</tr>}@empty{<tr><td colspan="6" class="text-center text-slate-400 py-10">No hay transacciones para este reporte.</td></tr>}
</tbody></table></div>
<p class="text-xs text-slate-500 mt-4">@if(tab==='rotation'){Rotación de últimos 90 días = unidades vendidas confirmadas / promedio entre saldo inicial y final estimado desde movimientos.} @if(tab==='abc'){ABC propuesto por valor de ventas confirmadas, con umbrales 80 % y 95 %. Sin historial de ventas se muestra SIN_DATOS.}@else{Los montos provienen de la base de datos, no de registros de demostración.}</p>
<button class="btn-soft mt-4" (click)="exportCsv()" [disabled]="!rows.length">↓ Exportar CSV</button>
}</div>
`})export class ReportsPage implements OnInit{
 private service=inject(BusinessService);auth=inject(AuthService);tab='sales';rows:any[]=[];loading=true;error='';
 get tabs(){return [{key:'sales',label:'Ventas mensuales'},...(this.auth.user()?.role==='ADMIN'?[{key:'purchases',label:'Compras mensuales'},{key:'inventory',label:'Inventario valorizado'},{key:'abc',label:'Clasificación ABC'},{key:'rotation',label:'Rotación de stock (90 d)'}]:[])]}
 get tabTitle(){return this.tabs.find(t=>t.key===this.tab)?.label||this.tab}
 ngOnInit(){this.load()}
 async load(){this.loading=true;this.error='';try{this.rows=await this.service.report(this.tab)}catch(e:any){this.error=e.message}finally{this.loading=false}}
 exportCsv(){if(!this.rows.length)return;const columns=Object.keys(this.rows[0]);const escape=(x:any)=>'"'+String(x??'').replace(/"/g,'""')+'"';const csv=[columns.map(escape).join(','),...this.rows.map(r=>columns.map(c=>escape(r[c])).join(','))].join('\r\n');const blob=new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=`sgrv_${this.tab}.csv`;a.click();URL.revokeObjectURL(a.href)}
}
