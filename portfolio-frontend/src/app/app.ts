import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TOKEN_KEY } from './auth.interceptor';
import { PortfolioAnalytics, PortfolioApi, PortfolioPerformancePoint, PortfolioSummary, Stock, StockFundamentals, StockTechnicalAnalysis, Trade, UserProfile, WatchlistItem } from './portfolio-api.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly api = inject(PortfolioApi);

  readonly authenticated = signal(false);
  readonly registering = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly summary = signal<PortfolioSummary | null>(null);
  readonly analytics = signal<PortfolioAnalytics | null>(null);
  readonly stocks = signal<Stock[]>([]);
  readonly catalogueStocks = signal<Stock[]>([]);
  readonly fundamentals = signal<StockFundamentals | null>(null);
  readonly technicalAnalysis = signal<StockTechnicalAnalysis | null>(null);
  readonly trades = signal<Trade[]>([]);
  readonly watchlist = signal<WatchlistItem[]>([]);
  readonly profile = signal<UserProfile | null>(null);
  readonly performance = signal<PortfolioPerformancePoint[]>([]);
  readonly performanceRange = signal<'week' | 'month' | 'year' | 'all'>('month');
  readonly illustrativePerformance = signal(false);

  username = '';
  email = '';
  password = '';
  ticker = 'AAPL';
  quantity = 1;
  stockQuery = '';
  private catalogueRequest = 0;

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
    this.illustrativePerformance.set(false);
    this.trades.set([]);
    this.watchlist.set([]);
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
    }).subscribe({
      next: (data) => {
        this.summary.set(data.summary);
        this.profile.set(data.profile);
        this.username = data.profile.username;
        this.analytics.set(data.analytics);
        this.setPerformanceData(data.performance);
        this.stocks.set(data.stocks);
        this.catalogueStocks.set(data.stocks);
        this.trades.set(data.trades);
        this.watchlist.set(data.watchlist);
        if (!this.stocks().some((stock) => stock.ticker === this.ticker)) {
          this.ticker = this.stocks()[0]?.ticker ?? '';
        }
        this.loadStockAnalysis(this.ticker);
        this.busy.set(false);
        this.error.set('');
      },
      error: (failure: HttpErrorResponse) => {
        this.error.set(this.messageFor(failure));
        this.busy.set(false);
        if (failure.status === 401) this.logout();
      },
    });
  }

  addToWatchlist(stock: Stock): void {
    this.api.addWatchlist(stock.ticker).subscribe({
      next: () => {
        this.notice.set(`${stock.ticker} added to your watchlist.`);
        this.api.watchlist().subscribe((items) => this.watchlist.set(items));
      },
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
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
    this.api.trade(this.ticker, type, this.quantity).subscribe({
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
    if (recorded.length >= 2) {
      this.performance.set(recorded);
      this.illustrativePerformance.set(false);
      return;
    }

    const range = this.performanceRange();
    const currentValue = recorded.at(-1)?.totalValue ?? this.summary()?.totalAccountValue ?? 100000;
    const pointCount = range === 'week' ? 7 : range === 'month' ? 30 : range === 'year' ? 52 : 60;
    const totalReturn = range === 'week' ? 0.012 : range === 'month' ? -0.038 : range === 'year' ? 0.155 : 0.285;
    const amplitude = range === 'week' ? 0.004 : range === 'month' ? 0.013 : range === 'year' ? 0.025 : 0.04;
    const startValue = currentValue / (1 + totalReturn);
    const lastDate = recorded.length
      ? new Date(`${recorded[recorded.length - 1].date}T00:00:00Z`)
      : new Date();
    lastDate.setUTCHours(0, 0, 0, 0);
    const points = Array.from({ length: pointCount }, (_, index) => {
      const progress = index / (pointCount - 1);
      const trend = startValue + (currentValue - startValue) * progress;
      const oscillation = Math.sin(progress * Math.PI * (range === 'week' ? 3 : range === 'month' ? 8 : 12))
        * currentValue * amplitude;
      const date = new Date(lastDate);
      if (range === 'week' || range === 'month') {
        date.setUTCDate(date.getUTCDate() - (pointCount - 1 - index));
      } else if (range === 'year') {
        date.setUTCDate(date.getUTCDate() - (pointCount - 1 - index) * 7);
      } else {
        date.setUTCMonth(date.getUTCMonth() - (pointCount - 1 - index));
      }
      return {
        date: date.toISOString().slice(0, 10),
        cashBalance: 0,
        investedValue: 0,
        totalValue: Math.round((trend + oscillation) * 100) / 100,
        source: 'ILLUSTRATIVE_ONLY',
      };
    });
    this.performance.set(points);
    this.illustrativePerformance.set(true);
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
    this.api.searchStocks(query).subscribe({
      next: (items) => {
        if (request === this.catalogueRequest) this.catalogueStocks.set(items);
      },
      error: (failure) => this.error.set(this.messageFor(failure)),
    });
  }

  loadStockAnalysis(ticker: string): void {
    if (!ticker) return;
    forkJoin({
      fundamentals: this.api.fundamentals(ticker),
      technical: this.api.technicalAnalysis(ticker),
    }).subscribe({
      next: (data) => {
        this.fundamentals.set(data.fundamentals);
        this.technicalAnalysis.set(data.technical);
      },
      error: (failure) => this.error.set(this.messageFor(failure)),
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
        this.catalogueStocks.set(items);
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
    if (failure.status === 400) return detail ?? 'Check the username, email, and password, then try again.';
    if (failure.status === 0) return 'Could not reach PortfolioPro. Check that the backend is running on port 8081.';
    if (failure.status >= 500) return detail ?? 'The server could not complete the request. Please try again.';
    return detail ?? 'The request could not be completed. Please check the details and try again.';
  }
}
