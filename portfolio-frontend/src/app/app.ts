import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TOKEN_KEY } from './auth.interceptor';
import {
  MarketDataStatus,
  PortfolioAnalytics,
  PortfolioApi,
  PortfolioPerformancePoint,
  PortfolioSummary,
  Stock,
  StockFundamentals,
  StockSearchResult,
  StockTechnicalAnalysis,
  Trade,
  UserProfile,
  WatchlistItem,
} from './portfolio-api.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly api = inject(PortfolioApi);
  private readonly router = inject(Router);

  readonly authenticated = signal(false);
  readonly registering = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly summary = signal<PortfolioSummary | null>(null);
  readonly analytics = signal<PortfolioAnalytics | null>(null);
  readonly stocks = signal<Stock[]>([]);
  readonly catalogueStocks = signal<StockSearchResult[]>([]);
  readonly fundamentals = signal<StockFundamentals | null>(null);
  readonly technicalAnalysis = signal<StockTechnicalAnalysis | null>(null);
  readonly stockAnalysisLoading = signal(false);
  readonly trades = signal<Trade[]>([]);
  readonly watchlist = signal<WatchlistItem[]>([]);
  readonly profile = signal<UserProfile | null>(null);
  readonly performance = signal<PortfolioPerformancePoint[]>([]);
  readonly performanceRange = signal<'week' | 'month' | 'year' | 'all'>('month');
  readonly marketDataStatus = signal<MarketDataStatus | null>(null);

  activityFilter: 'ALL' | 'BUY' | 'SELL' = 'ALL';
  activityPage = 1;
  readonly activityPageSize = 6;

  username = '';
  email = '';
  password = '';
  ticker = 'AAPL';
  quantity = 1;
  stockQuery = '';
  private catalogueRequest = 0;
  private catalogueSearchTimeout?: ReturnType<typeof setTimeout>;
  protected preserveTickerSelection = false;

  ngOnInit(): void {
    if (sessionStorage.getItem(TOKEN_KEY)) {
      this.authenticated.set(true);
      this.refresh();
    } else {
      this.loadStocks();
    }
  }

  submitAuth(): void {
    this.error.set('');
    this.notice.set('');
    this.busy.set(true);
    const request = this.registering()
      ? this.api.register(this.username, this.email, this.password)
      : this.api.login(this.username, this.password);
    request.subscribe({
      next: (result) => {
        sessionStorage.setItem(TOKEN_KEY, result.token);
        this.authenticated.set(true);
        this.password = '';
        this.busy.set(false);
        this.refresh();
      },
      error: (failure: HttpErrorResponse) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
      },
    });
  }

  toggleAuthMode(): void {
    this.registering.update((current) => !current);
    this.error.set('');
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.authenticated.set(false);
    this.summary.set(null);
    this.profile.set(null);
    this.analytics.set(null);
    this.performance.set([]);
    this.trades.set([]);
    this.watchlist.set([]);
    this.fundamentals.set(null);
    this.technicalAnalysis.set(null);
    this.stockAnalysisLoading.set(false);
    this.marketDataStatus.set(null);
    this.notice.set('');
    this.error.set('');
    this.password = '';
    this.loadStocks();
  }

  refresh(): void {
    this.busy.set(true);
    forkJoin({
      summary: this.api.summary(),
      profile: this.api.profile(),
      analytics: this.api.analytics(),
      performance: this.api.performance(this.performanceRange()),
      stocks: this.api.stocks(),
      trades: this.api.trades(),
      watchlist: this.api.watchlist(),
      marketDataStatus: this.api.marketDataStatus(),
    }).subscribe({
      next: (data) => {
        this.summary.set(data.summary);
        this.profile.set(data.profile);
        this.username = data.profile.username;
        this.analytics.set(data.analytics);
        this.setPerformanceData(data.performance);
        this.stocks.set(data.stocks);
        this.catalogueStocks.set(data.stocks.map((stock) => ({ ...stock, inCatalogue: true, primaryExchange: null })));
        this.trades.set(data.trades);
        this.watchlist.set(data.watchlist);
        this.marketDataStatus.set(data.marketDataStatus);
        if (!this.preserveTickerSelection && !this.stocks().some((stock) => stock.ticker === this.ticker)) {
          this.ticker = this.stocks()[0]?.ticker ?? '';
        }
        this.loadStockAnalysis(this.ticker);
        this.busy.set(false);
        this.error.set('');
      },
      error: (failure: HttpErrorResponse) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
        if (failure.status === 401 || failure.status === 404) this.logout();
      },
    });
  }

  addToWatchlist(stock: StockSearchResult): void {
    this.api.addWatchlist(stock.ticker).subscribe({
      next: () => {
        this.notice.set(`${stock.ticker} added to your watchlist.`);
        this.api.watchlist().subscribe((items) => this.watchlist.set(items));
      },
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
  }

  importStock(stock: StockSearchResult): void {
    this.busy.set(true);
    this.api.importStock(stock.ticker).subscribe({
      next: (added) => {
        this.ticker = added.ticker;
        this.notice.set(`${added.ticker} added with Massive end-of-day prices.`);
        this.busy.set(false);
        this.refresh();
        if (this.stockQuery.trim()) this.searchCatalogue(this.stockQuery);
      },
      error: (failure) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
      },
    });
  }

  refreshMarketData(): void {
    if (!this.marketDataStatus()?.configured) {
      this.notice.set('Add MASSIVE_API_KEY to the backend .env file to enable real end-of-day market prices.');
      return;
    }
    this.busy.set(true);
    this.api.refreshMarketData().subscribe({
      next: (result) => {
        this.notice.set(`Market data updated for ${result.updatedStocks} stocks${result.failedStocks ? `; ${result.failedStocks} failed` : ''}.`);
        this.refresh();
      },
      error: (failure) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
      },
    });
  }

  addPracticeFunds(): void {
    this.busy.set(true);
    this.api.addPracticeFunds().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.notice.set('Added $100,000 in simulated practice cash. No real money was added.');
        this.refresh();
      },
      error: (failure) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
      },
    });
  }

  get selectedStock(): Stock | undefined {
    const inCatalogue = this.catalogueStocks().find((stock) => stock.ticker === this.ticker);
    const current = this.stocks().find((stock) => stock.ticker === this.ticker)
      ?? (inCatalogue && inCatalogue.currentPrice != null
        ? {
            ticker: inCatalogue.ticker,
            companyName: inCatalogue.companyName,
            currentPrice: inCatalogue.currentPrice,
            priceType: inCatalogue.priceType,
            sector: inCatalogue.sector ?? 'Unknown',
            primaryExchange: inCatalogue.primaryExchange,
            priceUpdatedAt: inCatalogue.priceUpdatedAt ?? new Date().toISOString(),
          }
        : undefined);
    return current;
  }

  get estimatedOrderValue(): number {
    return (this.selectedStock?.currentPrice ?? 0) * (Number.isFinite(this.quantity) ? this.quantity : 0);
  }

  get selectedHoldingQuantity(): number {
    return this.summary()?.holdings.find((holding) => holding.ticker === this.ticker)?.quantity ?? 0;
  }

  get validTradeQuantity(): boolean {
    return Number.isInteger(this.quantity) && this.quantity > 0 && this.quantity <= 1_000_000;
  }

  get canBuy(): boolean {
    return this.validTradeQuantity && this.estimatedOrderValue > 0
      && this.estimatedOrderValue <= (this.summary()?.cashBalance ?? 0);
  }

  get canSell(): boolean {
    return this.validTradeQuantity && this.quantity <= this.selectedHoldingQuantity;
  }

  get tradeReadinessMessage(): string {
    if (!this.ticker) return 'Select a stock to preview the order.';
    if (!this.validTradeQuantity) return 'Enter a positive whole-share quantity.';
    if (!this.selectedStock) return 'The selected stock is not available in the catalogue.';
    if (!this.canBuy && !this.canSell) return 'Buy requires enough cash; sell requires enough owned shares.';
    if (!this.canBuy) return 'Buy is unavailable because the order exceeds available cash.';
    if (!this.canSell) return 'Sell is unavailable because you do not own enough shares.';
    return 'Both simulated order actions are ready.';
  }

  get hasRealMarketData(): boolean {
    return this.stocks().some((stock) => stock.priceType === 'MASSIVE_EOD')
      || this.catalogueStocks().some((stock) => stock.priceType === 'MASSIVE_EOD');
  }

  get marketPriceLabel(): string {
    return this.hasRealMarketData ? 'Real EOD prices' : 'Simulated demo prices';
  }

  get currentSection(): string {
    const path = this.router.url.split('?')[0];
    const section = path.split('/')[1];
    if (section === 'stock') return 'Stock detail';
    if (section === 'holdings') return 'Holdings';
    if (section === 'watchlist') return 'Watchlist';
    if (section === 'activity') return 'Activity';
    if (section === 'performance') return 'Performance';
    return 'Overview';
  }

  removeFromWatchlist(item: WatchlistItem): void {
    this.api.removeWatchlist(item.ticker).subscribe({
      next: () => this.watchlist.update((items) => items.filter((entry) => entry.ticker !== item.ticker)),
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
  }

  placeTrade(type: 'BUY' | 'SELL'): void {
    this.error.set('');
    this.notice.set('');
    this.busy.set(true);
    const idempotencyKey = crypto.randomUUID();
    this.api.trade(this.ticker, type, this.quantity, idempotencyKey).subscribe({
      next: (trade) => {
        this.notice.set(`${type === 'BUY' ? 'Bought' : 'Sold'} ${trade.quantity} ${trade.ticker} share${trade.quantity === 1 ? '' : 's'} at ${this.money(trade.priceAtExecution)}. This is a simulated trade.`);
        this.refresh();
      },
      error: (failure) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
      },
    });
  }

  get filteredTrades(): Trade[] {
    const filtered = this.activityFilter === 'ALL'
      ? this.trades()
      : this.trades().filter((trade) => trade.type === this.activityFilter);
    const start = (this.activityPage - 1) * this.activityPageSize;
    return filtered.slice(start, start + this.activityPageSize);
  }

  get activityTotalPages(): number {
    const count = this.activityFilter === 'ALL'
      ? this.trades().length
      : this.trades().filter((trade) => trade.type === this.activityFilter).length;
    return Math.max(1, Math.ceil(count / this.activityPageSize));
  }

  setActivityFilter(filter: 'ALL' | 'BUY' | 'SELL'): void {
    this.activityFilter = filter;
    this.activityPage = 1;
  }

  changeActivityPage(delta: number): void {
    this.activityPage = Math.min(this.activityTotalPages, Math.max(1, this.activityPage + delta));
  }

  money(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value ?? 0);
  }

  allocationStyle(): string {
    const values = this.analytics()?.sectors ?? [];
    const colors = ['#167c68', '#e4a74f', '#4573a8', '#a16db5', '#dc7654', '#7b91a3'];
    let cursor = 0;
    const slices = values.map((item, index) => {
      const start = cursor;
      cursor += item.allocationPercent;
      return `${colors[index % colors.length]} ${start}% ${cursor}%`;
    });
    if (cursor < 100) slices.push(`#e8ecea ${cursor}% 100%`);
    return `conic-gradient(${slices.join(', ') || '#e8ecea 0% 100%'})`;
  }

  technicalHistoryLine(): string {
    const values = this.technicalAnalysis()?.history.map((point) => point.close) ?? [];
    if (!values.length) return '';
    const min = Math.min(...values);
    const spread = Math.max(...values) - min;
    return values.map((value, index) => {
      const x = values.length === 1 ? 200 : 8 + (index * 384) / (values.length - 1);
      const y = spread === 0 ? 46 : 76 - ((value - min) / spread) * 60;
      return `${x},${y}`;
    }).join(' ');
  }

  performanceLine(): string {
    const values = this.performance().map((point) => point.totalValue);
    if (!values.length) return '';
    const min = Math.min(...values);
    const spread = Math.max(...values) - min;
    return values.map((value, index) => {
      const x = values.length === 1 ? 200 : 12 + (index * 376) / (values.length - 1);
      const y = spread === 0 ? 46 : 76 - ((value - min) / spread) * 60;
      return `${x},${y}`;
    }).join(' ');
  }

  setPerformanceRange(range: 'week' | 'month' | 'year' | 'all'): void {
    this.performanceRange.set(range);
    this.api.performance(range).subscribe({
      next: (points) => this.setPerformanceData(points),
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
  }

  private setPerformanceData(recorded: PortfolioPerformancePoint[]): void {
    this.performance.set(recorded);
  }

  performanceChange(): number {
    const points = this.performance();
    if (points.length < 2 || points[0].totalValue === 0) return 0;
    return ((points[points.length - 1].totalValue - points[0].totalValue) / points[0].totalValue) * 100;
  }

  performanceAmountChange(): number {
    const points = this.performance();
    if (points.length < 2) return 0;
    return points[points.length - 1].totalValue - points[0].totalValue;
  }

  searchCatalogue(query: string): void {
    this.stockQuery = query;
    const request = ++this.catalogueRequest;
    if (this.catalogueSearchTimeout) clearTimeout(this.catalogueSearchTimeout);
    this.catalogueSearchTimeout = setTimeout(() => {
      this.api.searchStocks(query).subscribe({
        next: (items) => {
          if (request === this.catalogueRequest) {
            this.catalogueStocks.set(items.map((stock) => ({
              ...stock,
              inCatalogue: stock.inCatalogue ?? true,
              primaryExchange: stock.primaryExchange ?? null,
            })));
          }
        },
        error: (failure) => this.error.set(this.messageFor(failure)),
      });
    }, 500);
  }

  selectStock(ticker: string): void {
    const normalizedTicker = ticker.trim().toUpperCase();
    if (!normalizedTicker) return;
    this.ticker = normalizedTicker;
    this.loadStockAnalysis(normalizedTicker);
    this.router.navigate(['/stock', normalizedTicker]);
  }

  loadStockAnalysis(ticker: string): void {
    const normalizedTicker = ticker.trim().toUpperCase();
    if (!normalizedTicker) return;
    this.ticker = normalizedTicker;
    this.fundamentals.set(null);
    this.technicalAnalysis.set(null);
    this.stockAnalysisLoading.set(true);
    forkJoin({
      fundamentals: this.api.fundamentals(normalizedTicker),
      technical: this.api.technicalAnalysis(normalizedTicker),
    }).subscribe({
      next: (data) => {
        this.fundamentals.set(data.fundamentals);
        this.technicalAnalysis.set(data.technical);
        this.stockAnalysisLoading.set(false);
      },
      error: (failure) => {
        this.stockAnalysisLoading.set(false);
        this.error.set(this.messageFor(failure));
      },
    });
  }

  compactMoney(value: number | null | undefined): string {
    if (value == null) return '—';
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 2 }).format(value);
  }

  private loadStocks(): void {
    this.api.stocks().subscribe({
      next: (items) => {
        this.stocks.set(items);
        this.catalogueStocks.set(items.map((stock) => ({ ...stock, inCatalogue: true, primaryExchange: null })));
        if (items.length) {
          this.ticker = items[0].ticker;
          this.loadStockAnalysis(this.ticker);
        }
      },
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
  }

  private messageFor(failure: HttpErrorResponse): string {
    const detail = failure.error?.message ?? failure.error?.detail;
    if (failure.status === 409) {
      return detail ?? 'An account with this username or email already exists. Sign in instead or use different details.';
    }
    if (failure.status === 401) return detail ?? 'Invalid username or password.';
    if (failure.status === 404) return 'Your previous local demo account was cleared when the backend restarted. Create the account again to continue.';
    if (failure.status === 400) return detail ?? 'Check the username, email, and password, then try again.';
    if (failure.status === 422) return detail ?? 'The request could not be completed with the current account balance or holdings.';
    if (failure.status === 0) return 'Could not reach PortfolioPro. Check that the backend is running on port 8081.';
    if (failure.status >= 500) return detail ?? 'The server could not complete the request. Please try again.';
    return detail ?? 'The request could not be completed. Please check the details and try again.';
  }
}