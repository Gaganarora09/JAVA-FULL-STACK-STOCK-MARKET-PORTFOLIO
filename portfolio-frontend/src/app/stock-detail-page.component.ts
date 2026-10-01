import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { App } from './app';

@Component({
  selector: 'app-stock-detail-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './stock-detail-page.component.html',
  styleUrl: './app.scss',
})
export class StockDetailPageComponent extends App implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected override preserveTickerSelection = true;

  override ngOnInit(): void {
    super.ngOnInit();
    this.route.paramMap.subscribe((params) => {
      const ticker = params.get('ticker')?.trim().toUpperCase();
      if (ticker) {
        this.ticker = ticker;
        this.loadStockAnalysis(ticker);
      }
    });
  }
}
